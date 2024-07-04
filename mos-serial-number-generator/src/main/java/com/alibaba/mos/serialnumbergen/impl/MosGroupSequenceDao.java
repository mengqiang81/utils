package com.alibaba.mos.serialnumbergen.impl;

import com.taobao.tddl.client.sequence.SequenceRange;
import com.taobao.tddl.client.sequence.exception.SequenceException;
import com.taobao.tddl.client.sequence.impl.GroupSequenceDao;
import com.taobao.tddl.client.sequence.util.RandomSequence;
import com.taobao.tddl.group.jdbc.TGroupDataSource;
import com.taobao.tddl.monitor.logger.LoggerInit;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MosGroupSequenceDao extends GroupSequenceDao {

    public Map<String, Integer> getSequenceMaxLength() {
        return sequenceMaxLength;
    }

    public void setSequenceMaxLength(Map<String, Integer> sequenceMaxLength) {
        this.sequenceMaxLength = sequenceMaxLength;
    }

    private Map<String, Integer> sequenceMaxLength = new HashMap<>();


    @Override
    public SequenceRange nextRange(final String name) throws SequenceException {
        if (isIgnoreInit()) {
            throw new SequenceException("start by in unit , so ignore");
        }

        if (name == null) {
            logger.error("序列名为空！");
            throw new IllegalArgumentException("序列名称不能为空");
        }

        configLock.lock();
        Throwable ex = null;
        try {
            int[] randomIntSequence = RandomSequence.randomIntSequence(dscount);
            for (int i = 0; i < retryTimes; i++) {
                for (int j = 0; j < dscount; j++) {
                    int index = randomIntSequence[j];
                    if (isOffState(dbGroupKeys.get(index)) || !recoverFromExcludes(index)) {
                        continue;
                    }
                    final TGroupDataSource tGroupDataSource = getGroupDsByIndex(index);

                    boolean isRejectSeq = checkNeedRejectSequence(tGroupDataSource);
                    if (isRejectSeq) {
                        logger
                                .warn(String.format("reject get sequence from current group, groupKey is %s, appName is %s",
                                        tGroupDataSource.getDbGroupKey(),
                                        tGroupDataSource.getAppName()));
                        continue;
                    }

                    long newValue = 0L;
                    if (optimisticLockMode) {

                        long oldValue;
                        // 查询，只在这里做数据库挂掉保护和慢速数据库保护
                        try {
                            oldValue = getOldValue(tGroupDataSource, name);
                            if (!isOldValueFixed(oldValue)) {
                                continue;
                            }
                        } catch (SQLException e) {
                            ex = e;
                            logger.warn("取范围过程中--查询出错！" + dbGroupKeys.get(index) + ":" + name, e);
                            excludeDataSource(index);
                            continue;
                        }

                        newValue = generateNewValue(index, oldValue, name);
                        try {
                            if (0 == updateNewValue(tGroupDataSource, name, oldValue, newValue)) {
                                logger.warn("取范围过程中--乐观锁失败" + dbGroupKeys.get(index) + ":" + name);
                                continue;
                            }
                        } catch (SQLException e) {
                            ex = e;
                            logger.warn("取范围过程中--更新出错！" + dbGroupKeys.get(index) + ":" + name, e);
                            continue;
                        }
                    } else {
                        try {
                            newValue = getNewValueForNextRange(tGroupDataSource, name, index);
                        } catch (SQLException e) {
                            ex = e;
                            logger.warn("Failed to get new value for group sequence '" + name + "' on group '"
                                            + dbGroupKeys.get(index) + "'.",
                                    e);
                            excludeDataSource(index);
                            continue;
                        }
                    }

//                    SequenceRange range = new SequenceRange(newValue + 1, newValue + innerStep);
                    //仅改变这一个位置
                    SequenceRange range = checkAndResume(name, newValue);

                    String infoMsg = "Got a new range for group sequence '" + name + "'. Range Info: "
                            + range.toString();
                    LoggerInit.TDDL_SEQUENCE_LOG.info(infoMsg);
                    if (logger.isDebugEnabled()) {
                        logger.debug(infoMsg);
                    }

                    return range;
                }
                // 当还有最后一次重试机会时,清空excludedMap,让其有最后一次机会
                if (i == (retryTimes - 2)) {
                    excludedKeyCount.clear();
                }
            }

            if (ex == null) {
                logger.error("所有数据源都不可用！且重试" + this.retryTimes + "次后，仍然失败!请往上翻日志，查看具体失败的原因");
                throw new SequenceException("All dataSource faild to get value!");
            } else {
                logger.error("所有数据源都不可用！且重试" + this.retryTimes + "次后，仍然失败!", ex);
                throw new SequenceException(ex, ex.getMessage());
            }
        } finally {
            configLock.unlock();
        }
    }

    private SequenceRange checkAndResume(String name, long newValue) {
        long rangeMax = newValue + innerStep;

        Integer seqLength = sequenceMaxLength.get(name);
        //存在长度限制
        if (seqLength != null) {
            //长度超限，归零
            if (String.valueOf(newValue).length() > seqLength) {
                newValue = newValue % (long) Math.pow(10, seqLength);
                rangeMax = newValue + innerStep;
            }
            //rangeMax超限
            if (String.valueOf(rangeMax).length() > seqLength) {
                rangeMax--;
            }
        }

        SequenceRange range = new SequenceRange(newValue + 1, rangeMax);
        return range;
    }
}
