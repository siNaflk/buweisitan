package com.xunye.service;
/** 浏览计数聚合；不处理余额等资金信息。 */
public interface BookVisitService { void record(Long bookId); }
