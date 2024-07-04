package com.alibaba.mos.serialnumbergen

import com.alibaba.mos.serialnumbergen.impl.DefaultSerialNumberGenerator
import com.alibaba.mos.serialnumbergen.impl.MosGroupSequenceDao
import com.taobao.tddl.client.sequence.exception.SequenceException
import com.taobao.tddl.client.sequence.impl.GroupSequence
import com.taobao.tddl.client.sequence.util.SequenceHelper
import com.taobao.tddl.common.GroupDataSourceRouteHelper
import com.taobao.tddl.group.jdbc.TGroupDataSource
import org.springframework.util.ReflectionUtils
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by xinling on 2019-02-13.
 **/
class SerialNumberGeneratorSpec extends Specification {

    MosGroupSequenceDao sequenceDao

    GroupSequence sequence

    def setup() {
        sequenceDao = new MosGroupSequenceDao();
        sequenceDao.setAdjust(true);
        List<String> groups = new ArrayList();
        groups.add("SERIAL_NUMBER_GENERATOR_0000_GROUP");
        sequenceDao.setDbGroupKeys(groups);
        sequenceDao.setDscount(groups.size());
        sequenceDao.setAppName("SERIAL_NUMBER_GENERATOR_APP");

        sequence = new GroupSequence();
        sequence.setSequenceDao(sequenceDao);
        sequence.setName("test" + UUID.randomUUID().toString()[0..15])
    }

    def cleanup() {

        cleanupSequence(sequence)

    }

    def private cleanupSequence(GroupSequence groupSequence) {
        def dbGroupKeysField = ReflectionUtils.findField(MosGroupSequenceDao.class, 'dbGroupKeys')
        dbGroupKeysField.setAccessible(true)
        def dbGroupKeys = dbGroupKeysField.get(groupSequence.sequenceDao) as List<String>

        def dataSourceMapField = ReflectionUtils.findField(MosGroupSequenceDao.class, 'dataSourceMap')
        dataSourceMapField.setAccessible(true)
        def dataSourceMap = dataSourceMapField.get(groupSequence.sequenceDao) as Map<String, DataSource>
        if (!dataSourceMap) return

        for (int i = 0; i < dbGroupKeys.size(); i++) {
            if (dbGroupKeys.get(i).toUpperCase().endsWith("-OFF"))// 已经关掉，不处理
            {
                continue;
            }
            TGroupDataSource tGroupDataSource = (TGroupDataSource) dataSourceMap.get(dbGroupKeys.get(i));
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = tGroupDataSource.getConnection();
                stmt = conn.prepareStatement("delete from sequence where name=?");
                stmt.setString(1, groupSequence.getName());
                GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
                stmt.executeUpdate();
            } catch (Exception e) {
                throw new SequenceException(e)
            } finally {
                SequenceHelper.closeDbResources(rs, stmt, conn);
            }
        }
    }

    void "这是一个基本测试"() {
        setup:
        sequenceDao.init()

        try {
            sequence.init()
        } catch (Exception e) {
            e.printStackTrace()
        }

        def sng = new DefaultSerialNumberGenerator(sequence)
        sng.setUseCurrentDate(true);
        sng.setDateFormat("yyyyMMdd");
        sng.setPrefix("--");
        sng.setPostfix("==");

        expect:
        println sng.next()
        sng.next() != null

    }

    void "超过 maxLo 取值的测试"() {
        setup:
        sequenceDao.setInnerStep(10)
        sequenceDao.init()


        try {
            sequence.init()
        } catch (Exception e) {
            e.printStackTrace()
        }

        def sng = new DefaultSerialNumberGenerator(sequence)
        def result = (1..15).collect {
            sng.next()
        }
        expect:
        println(result)
        result == ['11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25']

    }

    void "测试起始位置"() {
        setup:
        sequenceDao.setInnerStep(10)
        sequenceDao.init()


        try {
            sequence.init()
        } catch (Exception e) {
            e.printStackTrace()
        }

        def sng = new DefaultSerialNumberGenerator(sequence)
        def result = sng.next()
        expect:
        println(result)
        result == '11'
    }

    void "测试归零"() {
        setup:
        def innerStrep = 10
        def seqLength = 1

        sequenceDao.setInnerStep(innerStrep)
        sequenceDao.init()

        try {
            sequence.init()
        } catch (Exception e) {
            e.printStackTrace()
        }

        def sng = new DefaultSerialNumberGenerator(sequence)
        sng.setSeqLength(seqLength)
        def result = (1..15).collect {
            sng.next()
        }
        expect:
        println(result)
        result == ['1', '2', '3', '4', '5', '6', '7', '8', '9', '1', '2', '3', '4', '5', '6']
    }

    //不支持可设置起始位置，本测试无意义
    void "测试一个比较大的起始位置的归零问题"() {
        setup:

        expect:
        1 == 1
    }

    void "测试常规算法"() {
        setup:
        sequenceDao.setInnerStep(10)
        sequenceDao.init()

        try {
            sequence.init()
        } catch (Exception e) {
            e.printStackTrace()
        }

        def sng = new DefaultSerialNumberGenerator(sequence)
        sng.setUseCurrentDate(true)
        sng.setDateFormat("yyyyMMddHH")
        sng.setZone("Asia/Shanghai")
        sng.setPrefix("--")
        sng.setPostfix("==")
        def result = sng.next()

        expect:
        println result
        result == '--' + OffsetDateTime.now(ZoneId.of('Asia/Shanghai')).format(DateTimeFormatter.ofPattern('yyyyMMddHH')) + '11' + '=='
    }

    void "测试多台机器情况下的归零"(){
        setup:
        sequenceDao.init()

        try {
            sequence.init()
        } catch (Exception e) {
            e.printStackTrace()
        }

        int nThreads = 10
        int innerStep = 10
        int seqLength = 1
        def sequenceName = sequence.getName()
        def result = new Vector()

        ExecutorService executor = Executors.newFixedThreadPool(nThreads)
        CyclicBarrier c = new CyclicBarrier(nThreads)
        CyclicBarrier c2 = new CyclicBarrier(nThreads + 1)

        for (int j=0; j<nThreads; j++) {
            Runnable r = new Runnable() {
                @Override
                void run() {
                    MosGroupSequenceDao dao = new MosGroupSequenceDao()
                    dao.setAdjust(true)
                    List<String> groups = new ArrayList()
                    groups.add("SERIAL_NUMBER_GENERATOR_0000_GROUP")
                    dao.setDbGroupKeys(groups)
                    dao.setDscount(groups.size())
                    dao.setAppName("SERIAL_NUMBER_GENERATOR_APP")
                    dao.setInnerStep(innerStep)
                    dao.init()

                    GroupSequence groupSequence = new GroupSequence()
                    groupSequence.setSequenceDao(dao)
                    groupSequence.setName(sequenceName)
                    try {
                        groupSequence.init()
                    } catch (Exception e) {
                        e.printStackTrace()
                    }

                    DefaultSerialNumberGenerator sng = new DefaultSerialNumberGenerator(groupSequence);
                    sng.setSeqLength(seqLength)

                    try {
                        //等待所有线程中的初始化工作完成
                        c.await()
                    } catch (InterruptedException e) {
                        e.printStackTrace()
                    } catch (BrokenBarrierException e) {
                        e.printStackTrace()
                    }
                    def result0 = (1..15).collect {
                        sng.next()
                    }
                    result.addAll(result0)
                    c2.await()
                }
            }
            executor.execute(r)
        }

        c2.await()
        executor.shutdown()

        expect:

        println result

        result == ["1","2","3","4","5","6","7","8","9","1","2","3","4","5","6",
                   "1","2","3","4","5","6","7","8","9","1","2","3","4","5","6",
                   "1","2","3","4","5","6","7","8","9","1","2","3","4","5","6",
                   "1","2","3","4","5","6","7","8","9","1","2","3","4","5","6",
                   "1","2","3","4","5","6","7","8","9","1","2","3","4","5","6",
                   "1","2","3","4","5","6","7","8","9","1","2","3","4","5","6",
                   "1","2","3","4","5","6","7","8","9","1","2","3","4","5","6",
                   "1","2","3","4","5","6","7","8","9","1","2","3","4","5","6",
                   "1","2","3","4","5","6","7","8","9","1","2","3","4","5","6",
                   "1","2","3","4","5","6","7","8","9","1","2","3","4","5","6"]

    }




}
