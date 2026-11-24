package com.xunye.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
/** M3仅对非资金浏览计数启用定时汇总；交易流水保持同步事务。 */
@Configuration @EnableScheduling
public class SchedulingConfig {}
