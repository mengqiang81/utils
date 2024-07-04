import com.taobao.hsf.lightapi.ServiceFactory
import com.yintai.serialnumbergen.Application
import com.yintai.serialnumbergen.service.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootContextLoader
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
/**
 * Created by Qiang on 21/12/2016.
 */
@WebAppConfiguration
@ActiveProfiles(value = "testing")
@ContextConfiguration(classes = [Application], loader = SpringBootContextLoader)
class SeriaNumberGeneratorServiceSpec extends Specification {
    private static ServiceFactory factory = ServiceFactory.getInstanceWithPath("/Users/QiangWork/Workspace/intime/taobao/taobao-tomcat-production-7.0.59.3/deploy/")

    @Autowired
    private SerialNumberGeneratorService serialNumberGeneratorService

    @Autowired
    private GenerationRuleRepos generationRuleRepos

    @Autowired
    private SeriaNumNextHiRepos seriaNumNextHiRepos

    @Autowired
    private GenerationRuleService generationRuleService

    @Autowired
    private HiloAlgorithmServiceImpl hiloAlgorithmService

    def setup() {
        generationRuleService.create(new GenerationRuleDTO(
                algorithm: AlgorithmEnum.HILO,
                ruleCode: 'test',
                description: 'description',
                algorithmParams: new HiloAlgorithmDTO(unionCode: 'test', numberLength: 6, maxLo: 20)
        ))
        generationRuleService.create(new GenerationRuleDTO(
                algorithm: AlgorithmEnum.HILO,
                ruleCode: 'test2',
                description: 'description',
                algorithmParams: new HiloAlgorithmDTO(unionCode: 'test2', numberLength: 6, maxLo: 20, start: 100000)
        ))
        generationRuleService.create(new GenerationRuleDTO(
                algorithm: AlgorithmEnum.HILO,
                ruleCode: 'test3',
                description: 'description',
                algorithmParams: new HiloAlgorithmDTO(unionCode: 'test3', numberLength: 1, maxLo: 20)
        ))
        generationRuleService.create(new GenerationRuleDTO(
                algorithm: AlgorithmEnum.HILO,
                ruleCode: 'test4',
                description: 'description',
                algorithmParams: new HiloAlgorithmDTO(unionCode: 'test4', numberLength: 1, maxLo: 20, start: 8)
        ))
        generationRuleService.create(new GenerationRuleDTO(
                algorithm: AlgorithmEnum.COMMON,
                ruleCode: 'test5',
                description: 'description',
                algorithmParams: new CommonAlgorithmDTO(unionCode: 'test5', numberLength: 6, maxLo: 20, zone: 'Asia/Shanghai', dateFormat: 'yyyyMMddHH', prefix: 'TT')
        ))
//        generationRuleService.create(new GenerationRuleDTO(
//                algorithm: AlgorithmEnum.HILO,
//                ruleCode: 'test6',
//                description: 'description',
//                algorithmParams: new HiloAlgorithmDTO(unionCode: 'test6', numberLength: 6, maxLo: 10, start: -1L)
//        ))
    }

    def cleanup() {
        seriaNumNextHiRepos.deleteByChannel('test')
        seriaNumNextHiRepos.deleteByChannel('test2')
        seriaNumNextHiRepos.deleteByChannel('test3')
        seriaNumNextHiRepos.deleteByChannel('test4')
        seriaNumNextHiRepos.deleteByChannel('test5')
        generationRuleRepos.deleteByRuleCode('test')
        generationRuleRepos.deleteByRuleCode('test2')
        generationRuleRepos.deleteByRuleCode('test3')
        generationRuleRepos.deleteByRuleCode('test4')
        generationRuleRepos.deleteByRuleCode('test5')
        //TODO 这里利用了动态语言的特性来清空内存
        hiloAlgorithmService.concurrentMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue>()
    }

    void "一个基准测试"() {
        setup:
        def result = serialNumberGeneratorService.genNumber("test")
        expect:
        result == '000001'
    }

    void "超过 maxLo 取值的测试"() {
        setup:
        def result = (1..25).collect {
            serialNumberGeneratorService.genNumber("test")
        }.collect()
        expect:
        result == ['000001', '000002', '000003', '000004', '000005', '000006', '000007', '000008', '000009', '000010', '000011', '000012', '000013', '000014', '000015', '000016', '000017', '000018', '000019', '000020', '000021', '000022', '000023', '000024', '000025']
    }

    void "测试起始位置"() {
        setup:
        def result = serialNumberGeneratorService.genNumber("test2")
        expect:
        result == '100001'
    }

    void "测试归零"() {
        setup:
        def result = (1..15).collect {
            serialNumberGeneratorService.genNumber("test3")
        }.collect()
        println DateTimeZone.getAvailableIDs()
        expect:
        result == ['1', '2', '3', '4', '5', '6', '7', '8', '9', '1', '2', '3', '4', '5', '6']
    }

    void "测试一个比较大的起始位置的归零问题"() {
        setup:
        def result = (1..15).collect {
            serialNumberGeneratorService.genNumber("test4")
        }.collect()
        expect:
        result == ['9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9']
    }

    void "测试常规算法"() {
        setup:
        def result = serialNumberGeneratorService.genNumber("test5")
        println result
        expect:
        result == 'TT' + new DateTime(DateTimeZone.forID('Asia/Shanghai')).toString('yyyyMMddHH') + '000001'
    }

    // TODO
//    void "测试多台机器情况下的归零"(){
//
//    }

    //TODO
//    @Ignore
//    void "测试随机数 start"(){
//        setup:
//        def result = (1..50).collect {
//            serialNumberGeneratorService.genNumber("test6")
//        }.collect().each {
//            println it
//        }
//        expect:
//        1==1
//    }
}
