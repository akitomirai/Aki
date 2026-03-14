package edu.jxust.agritrace.module.notary.service;

public interface HashNotaryService {

    void notarize(String bizType, Long bizId, String snapshot, Long operatorId, String remark);

    String sha256(String content);
}