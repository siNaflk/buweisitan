package com.xunye.service.impl;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xunye.dao.BookMapper;
import com.xunye.entity.Book;
import com.xunye.service.BookVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
/** 统计允许短暂延迟，余额不经过此路径。Redis原子取走批次，多个实例不会重复刷同一批。 */
// @Service：把该实现类交给Spring管理，Controller注入Service接口时可获得这个Bean。
@Service
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
// @Slf4j：Lombok生成log日志对象，供log.warn等语句记录运行情况。
@Slf4j
public class BookVisitServiceImpl implements BookVisitService {
    private static final String KEY="Cache::XunYe::visit:pending";
    private final StringRedisTemplate redis;
    private final BookMapper books;
    // @Value：读取配置 xunye.stats.redis-enabled，未配置时使用 true。
    @Value("${xunye.stats.redis-enabled:true}")
    private boolean enabled;
    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public void record(Long id) {
        if(enabled) {
            try {redis.opsForHash().increment(KEY,id.toString(),1);return;}
            catch(org.springframework.dao.DataAccessException e){log.warn("浏览量计数Redis不可用，回退数据库");}
        }
        increment(id,1);
    }
    /** 每分钟汇总；与余额流水无关。进程在取走计数后崩溃可能损失该批统计，比赛展示可接受。 */
    // @Scheduled：启动后等待配置的毫秒数再首次执行；此后每次执行结束，再等待fixedDelayString指定的间隔。
    // ${xunye.stats.flush-ms:60000}表示优先读取配置，缺省60000毫秒，即一分钟；需由@EnableScheduling启用调度。
    @Scheduled(fixedDelayString="${xunye.stats.flush-ms:60000}",initialDelayString="${xunye.stats.flush-ms:60000}")
    public void flush() {
        if(!enabled)return;
        try {
            for(Object field:redis.opsForHash().keys(KEY)) {
                String id=field.toString();
                Long count=redis.execute(new DefaultRedisScript<>("local v=redis.call('hget',KEYS[1],ARGV[1]); redis.call('hdel',KEYS[1],ARGV[1]); return tonumber(v) or 0",Long.class),List.of(KEY),id);
                if(count==null || count<=0)continue;
                try {increment(Long.valueOf(id),count);}
                catch(RuntimeException e){redis.opsForHash().increment(KEY,id,count);log.warn("浏览量写入失败，计数已归还",e);}
            }
        }catch(RuntimeException e){log.warn("浏览量汇总暂不可用，下次重试",e);}
    }
    private void increment(Long id,long count) {
        books.update(null,new UpdateWrapper<Book>().eq("id",id).setSql("visit_count = visit_count + {0}",count));
    }
}
