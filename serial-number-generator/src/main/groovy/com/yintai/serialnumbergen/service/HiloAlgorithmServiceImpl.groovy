package com.yintai.serialnumbergen.service

import com.taobao.eagleeye.EagleEye
import com.yintai.serialnumbergen.domain.SerialNumNextHi
import groovy.transform.CompileStatic
import org.nofdev.logging.CustomLogger
import org.nofdev.servicefacade.ServiceContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.support.JdbcDaoSupport
import org.springframework.stereotype.Service
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

import javax.sql.DataSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap

/**
 * Created by Qiang on 27/04/2017.
 */
@Service
@CompileStatic
class HiloAlgorithmServiceImpl extends JdbcDaoSupport implements AlgorithmService<HiloAlgorithmDTO> {

    @Autowired
    private TransactionTemplate transactionTemplate

    private static final CustomLogger log = CustomLogger.getLogger(SerialNumberGeneratorServiceImpl.class)

    ConcurrentMap<String, ConcurrentLinkedQueue<Long>> concurrentMap = new ConcurrentHashMap()

    @Autowired
    public HiloAlgorithmServiceImpl(DataSource dataSource) {
        setDataSource(dataSource);
    }

    @Autowired
    private SeriaNumNextHiRepos seriaNumNextHiRepos

    @Override
    String gen(HiloAlgorithmDTO nextHiDTO) {

        String unionCode = nextHiDTO.unionCode
        Integer maxLo = nextHiDTO.maxLo
        Integer numberLength = nextHiDTO.numberLength
        Long start = nextHiDTO.start ? nextHiDTO.start : 0L
        //TODO 支持随机数 start
//        if(start == -1L){
//            start = new Random().nextInt(10^numberLength)
//        }

        def serialNumID = poll(nextHiDTO.unionCode)

        if (!serialNumID) {
            offer(unionCode, maxLo, start)
            serialNumID = poll(unionCode)
        }
        log.info() { "queue poll value:${serialNumID}" }

        //TODO 可以支持不填零
        def result = doZero(numberLength, serialNumID, unionCode, maxLo, start)

        log.info() { "return '${unionCode}' number :${result}" }

        return result
    }

    @Override
    void init(HiloAlgorithmDTO algorithmParams) {
        seriaNumNextHiRepos.save(new SerialNumNextHi(nextHi: 0L, channel: algorithmParams.unionCode))

    }
/**
 * 写入队列
 */
    private void offer(String unionCode, Integer maxLo, Long start) {

        def nextHi = this.getCurrentNextHi(unionCode)//得到当前nextHi

        log.info() { "start [nextHi] :${nextHi}" }

        def key = getRealKeyForPressure(unionCode)
        for (int i = 1; i <= maxLo; i++) {
            def writeValue = nextHi * maxLo + i + start
            if (!concurrentMap.get(key)) {
                concurrentMap.put(key, new ConcurrentLinkedQueue<Long>())
            }
            concurrentMap.get(key).offer(writeValue)
        }
        this.updateSerialNumNextHi(unionCode)//循环到maxlo,nextHi+1
    }

    /**
     * 从队列取值 fifo
     * @param unionCode
     * @return
     */
    private Long poll(String unionCode) {
        def key = getRealKeyForPressure(unionCode)
        def concurrentLinkedQueue = concurrentMap.get(key)
        return concurrentLinkedQueue?.poll()
    }

    /**
     * 清队列
     * @param unionCode
     * @return
     */
    private void resetSerialNumQueue(String unionCode, int numberLength, Integer maxLo, Long start) {
        log.info() { "'${unionCode}' return  number value  超过最大值开始清零" }
        def key = getRealKeyForPressure(unionCode)
        concurrentMap.get(key)?.clear()
        this.clearZeroSerialNumNextHi(unionCode, numberLength, maxLo, start)
    }

    /**
     * 数字补零
     * @param numberLength
     * @param serialNumID
     * @param unionCode
     * @param maxLo
     * @return
     */
    private String doZero(int numberLength, Long serialNumID, String unionCode, Integer maxLo, Long start) {
        //zero fill

        int serialNumIDLength = serialNumID.toString().length();
        if (serialNumIDLength < numberLength) {
            String result0 = zerofill(serialNumIDLength, numberLength);
            if (result0 != null) return result0 + serialNumID;
        } else if (serialNumIDLength > numberLength) {//归零后重取
            resetSerialNumQueue(unionCode, numberLength, maxLo, start)
            offer(unionCode, maxLo, start)
            return poll(unionCode)
        } else if (serialNumIDLength == numberLength) {
            return serialNumID;
        }
        return null;
    }

    /**
     * zero fill
     * @param idLength
     * @param numberLength
     * @return
     */
    private String zerofill(int idLength, int numberLength) {


        int iL = numberLength - idLength;
        String result0 = "";
        for (int i = 0; i < iL; i++) {
            result0 += "0";
        }
        return result0;
    }

    /**
     * 循环lo到maxlo时,更新nextHi=nextHi+1 ,得到最新的nextHi
     * @param unionCode
     * @return
     */
    private void updateSerialNumNextHi(String unionCode) {
        def objs = new Object[1]
        objs[0] = unionCode
        getJdbcTemplate().queryForObject("select  next_hi from  serial_num_next_hi   where channel =? for update ", objs, java.lang.Long.class);
        getJdbcTemplate().update("update serial_num_next_hi set next_hi=next_hi + 1  where channel =?", unionCode)
    }

    /**
     * set nextHi=0
     * @param unionCode
     * @return
     */
    private void clearZeroSerialNumNextHi(String unionCode, int numberLength, Integer maxLo, Long start) {
        /**
         * 这里要判断到底能不能清数据库, 因为别的服务器可能已经清过了
         * 原来是用自己的缓存来判断是否清数据库, 现在是用数据库再判断一遍是否可能溢出防止清别人已经清过的
         * 用事务, 防止并发, 这里到是确实可以用乐观锁, 但是这个锁只能在清零的时候+1, 鉴于清零的并发性不大, 事务损耗也不大, 所以还是用事务简单点
         */
        def objs = new Object[1]
        objs[0] = unionCode
        transactionTemplate.execute(new TransactionCallback() {
            @Override
            Object doInTransaction(TransactionStatus transactionStatus) {
                Long nextHi = getJdbcTemplate().queryForObject("select  next_hi from  serial_num_next_hi   where channel =? for update ", objs, java.lang.Long.class);
                int maxSerialNumIDLength = (nextHi * maxLo + maxLo + start).toString().length();
                if (maxSerialNumIDLength > numberLength) {
                    getJdbcTemplate().update("update serial_num_next_hi set next_hi=0  where channel =?", unionCode)
                } else {
                    log.info() { "'${unionCode}' 因为已经取到 ${nextHi} 没有实际清零数据库" }
                }
                return null
            }
        })

    }

    /**
     * 得到当前的nextHi
     * @param channel
     * @return
     */
    Long getCurrentNextHi(String channel) {
        def objs = new Object[1]
        objs[0] = channel
        Long result = getJdbcTemplate().queryForObject("select  next_hi from  serial_num_next_hi   where channel =? for update ", objs, java.lang.Long.class);
        return result
    }

    /**
     * 获取真正的缓存 key, 因为压测标的存在
     * @param unionCode
     * @return
     */
    private String getRealKeyForPressure(String unionCode) {
        log.debug("压测标 ${EagleEye.getUserData('t')}")
        def key = unionCode
        if (EagleEye.getUserData('t') == "1") {
            key = 'test_' + unionCode
        }
        key
    }
}
