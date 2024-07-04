package com.alibaba.mos.serialnumbergen.impl;

import com.alibaba.mos.serialnumbergen.SerialNumberGenerator;
import com.taobao.tddl.client.sequence.Sequence;
import com.taobao.tddl.client.sequence.exception.SequenceException;
import com.taobao.tddl.client.sequence.impl.GroupSequence;
import lombok.Data;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 流水号生成器
 */
@Data
public class DefaultSerialNumberGenerator implements SerialNumberGenerator {
    private static final int SEQ_LENGTH_MAX = 19;
    private static final int SEQ_LENGTH_MIN = 1;


    private Sequence sequence;

    public void setSeqLength(Integer seqLength) {
        if (seqLength != null)  {
            if (seqLength < SEQ_LENGTH_MIN || seqLength > SEQ_LENGTH_MAX) {
                throw new SequenceException(String.format("序列号长度必须在区间[%s, %s]之中",SEQ_LENGTH_MAX, SEQ_LENGTH_MIN));
            }
            //put seqName-maxLenth to dao
            GroupSequence groupSequence = (GroupSequence) sequence;
            ((MosGroupSequenceDao) groupSequence.getSequenceDao()).getSequenceMaxLength().put(groupSequence.getName(), seqLength);
        }
        this.seqLength = seqLength;
    }

    /**
     * 指定序列号长度,
     * 注意这个不是流水号的总长度, 而是去除了日期和前后缀后的长度,
     * 不够长度自动填零,
     * 按长度归零：达到指定长度后会归零, 所以要自己注意在 dateformat 的时间段内, 是否会重复的问题.
     * 有效取值范围是[19, 1]
     * 默认长度不限
     */
    private Integer seqLength;

    /**
     * 是否使用当前日期组合流水号字符串,
     * 如果不使用的话, 就是普通的序列号
     * 注意, 当前日期为服务器时间, 跟序列号不同, 日期在分布式环境中的一致性是由应用自己保证的. 所以请结合服务器间的时间差异设计流水号
     * 默认不使用
     */
    private boolean useCurrentDate = false;
    /**
     * 当前日期加序列号作为流水号时, 要指定日期格式,
     * 使用标准的DateTimeFormatter来描述,
     * 默认 yyyyMMdd
     * @see DateTimeFormatter
     */
    private String dateFormat ="yyyyMMdd";
    /**
     * 当前日期加序列号作为流水号时, 要指定日期时区,
     * 使用标准的ZoneId来描述, eg. Asia/Shanghai,
     * 默认为 Z
     * @see ZoneId
     */
    private String zone = "Z";
    /**
     * 流水号前缀
     */
    private String prefix;
    /**
     * 流水号后缀
     */
    private String postfix;

    /**
     * 构造器,
     * 目前只支持基于TDDL的GroupSequence
     */
    public DefaultSerialNumberGenerator(GroupSequence sequence) {
        this.sequence = sequence;
    }

    @Override
    public String next() {
        if (sequence == null) {
            throw new SerialNumberException("必须设置Sequence");
        }
        return buildStr(
            this.prefix,
            dateStr(this.useCurrentDate, this.zone, this.dateFormat),
            sequenceStr(),
            postfix);
    }


    private String sequenceStr() {
        long next = sequence.nextValue();
        String res = String.valueOf(next);
        //不够长度，填零补位
        if (seqLength != null && res.length() < seqLength) {
            res = seqFillZore(next);
        }
        return res;
    }

    private String seqFillZore(long next) {
        int iL = seqLength - String.valueOf(next).length();
        StringBuilder result0 = new StringBuilder();
        for (int i = 0; i < iL; i++) {
            result0.append("0");
        }
        return result0.append(next).toString();
    }

    private String dateStr(boolean useCurrentDate, String zone, String dateFormat) {
        if (!useCurrentDate) {
            return null;
        } else {
            try {
                return OffsetDateTime.now(ZoneId.of(zone)).format(DateTimeFormatter.ofPattern(dateFormat));
            } catch (Exception e) {
                throw new SerialNumberException("日期格式不正确，请检查");
            }
        }
    }

    private String buildStr(String prefix, String dateStr, String sequenceStr, String postfix) {
        prefix = prefix != null ? prefix.trim() : "";
        dateStr = dateStr != null ? dateStr.trim() : "";
        sequenceStr = sequenceStr != null ? sequenceStr.trim() : "";
        postfix = postfix != null ? postfix.trim() : "";
        return prefix + dateStr + sequenceStr + postfix;
    }

}
