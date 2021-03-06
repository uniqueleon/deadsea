package org.aztec.deadsea.xa.impl.redis.handler;

import java.util.List;
import java.util.Map;

import org.aztec.autumn.common.utils.CacheException;
import org.aztec.autumn.common.utils.CacheUtils;
import org.aztec.autumn.common.utils.JsonUtils;
import org.aztec.autumn.common.utils.UtilsFactory;
import org.aztec.autumn.common.utils.concurrent.NoLockException.ErrorCodes;
import org.aztec.deadsea.common.DeadSeaLogger;
import org.aztec.deadsea.common.xa.TransactionPhase;
import org.aztec.deadsea.common.xa.XAConstant;
import org.aztec.deadsea.common.xa.XAContext;
import org.aztec.deadsea.common.xa.XAExecutor;
import org.aztec.deadsea.common.xa.XAProposal;
import org.aztec.deadsea.common.xa.XAResponse;
import org.aztec.deadsea.common.xa.XAResponseBuilder;
import org.aztec.deadsea.xa.impl.redis.RedisTxMsgHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExecutorHandler implements RedisTxMsgHandler {

	@Autowired
	private List<XAExecutor> executors;
	private boolean unsubscribe = false;
	private CacheUtils cacheUtil;
	private JsonUtils jsonUtil;
	@Autowired
	private XAResponseBuilder responseBuilder;

	public ExecutorHandler() throws Exception {

		cacheUtil = UtilsFactory.getInstance().getDefaultCacheUtils();
		jsonUtil = UtilsFactory.getInstance().getJsonUtils();
	}
	
	public String getPublishChannel(TransactionPhase phase) {
		switch (phase) {
		case PREPARE:
			return  XAConstant.DEFAULT_REDIS_PUBLISH_CHANNELS[3];
		case COMMIT:
			return  XAConstant.DEFAULT_REDIS_PUBLISH_CHANNELS[4];
		case ROLLBACK:
			return  XAConstant.DEFAULT_REDIS_PUBLISH_CHANNELS[5];
		}
		return null;
	}

	@Override
	public void handle(XAProposal proposal,TransactionPhase currentPhase,XAContext context,
			Map<String,Object> dataMap) {
		// TODO Auto-generated method stub
		String pubChannel = getPublishChannel(currentPhase);
		try {
			DeadSeaLogger.info(XAConstant.LOG_KEY, "Execute for [" + proposal.getTxID() + "]");
			TransactionPhase phase = currentPhase;
			Integer assignmentNo = context.getAssignmentNo();
			String msg = null;
			boolean isFail = false;
			if(assignmentNo != null) {
				XAResponse response = null;

				for (XAExecutor executor : executors) {
					if(isFail) break;
					if(!executor.canHandle(context)) {
						continue;
					}
					switch (phase) {
					case PREPARE:
						response = executor.prepare(context);
						if(response.isFail()) {
							isFail = true;
						}
						context.persist();
						break;
					case COMMIT:
						context.setCurrentPhase(TransactionPhase.COMMIT);
						response = executor.commit(context);
						context.persist();
						break;
					case ROLLBACK:
						context.setCurrentPhase(TransactionPhase.ROLLBACK);
						response = executor.rollback(context);
						context.persist();
						break;
					}
				}
				msg = jsonUtil.object2Json(response);
				cacheUtil.publish(pubChannel, msg);
			}
			DeadSeaLogger.info(XAConstant.LOG_KEY, "Execute for [" + proposal.getTxID() + "] finished!");
		} catch (Exception e) {
			DeadSeaLogger.error("[XA]", e);
			XAResponse reponse = responseBuilder.buildFail(context.getTransactionID(), context.getAssignmentNo(), ErrorCodes.UNKONW_ERROR, currentPhase);
			try {
				cacheUtil.publish(pubChannel, jsonUtil.object2Json(reponse));
			} catch (Exception e1) {
				DeadSeaLogger.error("[XA]", e1);
			}
		} finally{
			
		}
	}
	

	public static String[] getChannelPrefix() {
		return new String[] { XAConstant.REDIS_CHANNLE_NAMES.PREPARE, XAConstant.REDIS_CHANNLE_NAMES.COMMIT,
				XAConstant.REDIS_CHANNLE_NAMES.ROLLBACK };
	}
	
	@Override
	public boolean accept(String channel,XAProposal proposal) {
		String[] channelPrefix = getChannelPrefix();
		for(String pref:channelPrefix) {
			if(channel.equals(pref)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isAssignable() {
		return true;
	}

}
