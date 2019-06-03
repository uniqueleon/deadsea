package org.aztec.deadsea.common.xa;

public interface XAResponseBuilder {

	public XAResponse buildSuccess(String tx,int assignmentNo,TransactionPhase phase);
	public XAResponse buildFail(String tx,int assignmentNo,Throwable t,TransactionPhase phase);
	public XAResponse buildFail(String tx,int assignmentNo,int errorCode,TransactionPhase phase);
	
}