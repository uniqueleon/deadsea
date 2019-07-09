package org.aztec.deadsea.sql.impl.xa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.aztec.deadsea.common.xa.XAContext;
import org.aztec.deadsea.common.xa.XAException;
import org.aztec.deadsea.common.xa.XAExecutor;
import org.aztec.deadsea.common.xa.XAResponse;
import org.aztec.deadsea.common.xa.XAResponseBuilder;
import org.aztec.deadsea.sql.impl.druid.DruidConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

@Component
public class XASQLExecutor implements XAExecutor {

	private static final Map<String, Connection> connections = Maps.newConcurrentMap();
	//private static final 
	@Autowired
	private XAResponseBuilder builder;

	public XASQLExecutor() {
		// TODO Auto-generated constructor stub
	}

	

	@Override
	public XAResponse prepare(XAContext context) throws XAException {
		// TODO Auto-generated method stub
		// Connection conn = get
		try {
			DruidConnector connector = new DruidConnector();
			Connection conn = connector.getConnection(getConnectArgs(context));
			connections.put(context.getTransactionID(), conn);
			conn.setAutoCommit(false);
			Statement stm = conn.createStatement();
			List<String> sqls = (List<String>) context.get(XAConstant.CONTEXT_KEYS.EXECUTE_SQL);
			for(String sql : sqls) {
				stm.execute(sql);
			}
			XAResponse response = builder.buildSuccess(context.getTransactionID(), context.getAssignmentNo(),
					context.getCurrentPhase());
			return response;
		} catch (Exception e) {
			XAResponse response = builder.buildFail(context.getTransactionID(), context.getAssignmentNo(),e,
					context.getCurrentPhase());
			return response;
		}
	}
	
	public String[] getConnectArgs(XAContext context) {
		List<List<String>> allArgs = (List<List<String>>)context.get(XAConstant.CONTEXT_KEYS.CONNECT_ARGS);
		List<String> retArgList =  allArgs.get(context.getAssignmentNo());
		return retArgList.toArray(new String[retArgList.size()]);
	}

	@Override
	public XAResponse commit(XAContext context) throws XAException {
		// TODO Auto-generated method stub
		try {
			Connection connection = connections.get(context.getTransactionID());
			connection.commit();
			XAResponse response = builder.buildSuccess(context.getTransactionID(), context.getAssignmentNo(),
					context.getCurrentPhase());
			return response;
		} catch (SQLException e) {
			XAResponse response = builder.buildFail(context.getTransactionID(), context.getAssignmentNo(), e,
					context.getCurrentPhase());
			return response;
		}
	}

	@Override
	public XAResponse rollback(XAContext context) throws XAException {
		try {
			Connection connection = connections.get(context.getTransactionID());
			connection.rollback();
			Object sqlObject = context.get(XAConstant.CONTEXT_KEYS.ROLLBACK_SQL);
			if(sqlObject != null) {
				List<String> sqlList = (List<String>) sqlObject;
				for(String sql : sqlList) {
					PreparedStatement ps = connection.prepareStatement(sql);
					ps.execute(sql);
				}
			}
			XAResponse response = builder.buildSuccess(context.getTransactionID(), context.getAssignmentNo(),
					context.getCurrentPhase());
			return response;
		} catch (SQLException e) {
			XAResponse response = builder.buildFail(context.getTransactionID(), context.getAssignmentNo(), e,
					context.getCurrentPhase());
			return response;
		}
	}

}
