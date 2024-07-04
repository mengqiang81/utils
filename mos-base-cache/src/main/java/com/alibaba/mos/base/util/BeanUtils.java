package com.alibaba.mos.base.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author huanglitao.hlt
 * @date 2020/03/20
 */
@Slf4j
public class BeanUtils {

    private static Map<Class,Map<String,Method>> methodCache = new ConcurrentHashMap<>();

    private static Map<Class,Map<String,Field>> fieldCache = new ConcurrentHashMap<>();

    private static final Map<Class,Class> primitiveReflect = new HashMap<>(8);

    static {
        primitiveReflect.put(boolean.class,Boolean.class);
        primitiveReflect.put(int.class,Integer.class);
        primitiveReflect.put(byte.class,Byte.class);
        primitiveReflect.put(double.class,Double.class);
        primitiveReflect.put(float.class,Float.class);
        primitiveReflect.put(long.class,Long.class);
        primitiveReflect.put(char.class,Character.class);
        primitiveReflect.put(short.class,Short.class);
        primitiveReflect.put(void.class,Void.class);
    }

    /**
     * 对象拷贝
     * @param source 源对象
     * @param targetClass 目标对象class
     * @param converters 自定义字段转换
     * @param <T> 返回目标对象类型
     * @return
     */
    public static <T> T copyBean(Object source, Class<T> targetClass, Map<String,Converter<?>> converters) {
        return copyBean(source, targetClass, null, converters, null);
    }

    /**
     * 对象拷贝
     * @param source 源对象
     * @param targetClass 目标对象class
     * @param <T> 返回目标对象类型
     * @return
     */
    public static <T> T copyBean(Object source, Class<T> targetClass) {
        return copyBean(source, targetClass,null, null, null);
    }

    public static <T> T copyBean(Object source, T targetObject) {
        return copyBean(source,(Class<T>)targetObject.getClass(),targetObject,null,null);
    }

    public static <T> T copyBean(Object source, T targetObject, Map<String,Converter<?>> converters) {
        return copyBean(source,(Class<T>)targetObject.getClass(),targetObject,converters,null);
    }

    public static <T> T copyBean(Object source, T targetObject, Map<String,Converter<?>> converters,List<String> noConverts) {
        return copyBean(source,(Class<T>)targetObject.getClass(),targetObject,converters,noConverts);
    }

    /**
     * 对象拷贝
     * @param source 源对象
     * @param targetClass 目标对象class
     * @param converters 自定义字段转换
     * @param noConverts 不需要转换的对象
     * @param <T> 返回目标对象类型
     * @return
     */
    public static <T> T copyBean(Object source, Class<T> targetClass, Map<String,Converter<?>> converters,List<String> noConverts) {
        return copyBean(source,targetClass,null,converters,noConverts);
    }

    /**
     * 对象拷贝
     * @param source 源对象
     * @param targetClass 目标对象class
     * @param targetObject 目标对象
     * @param converters 自定义字段转换
     * @param noConverts 不需要转换的对象
     * @param <T> 返回目标对象类型
     * @return
     */
    public static <T> T copyBean(Object source, Class<T> targetClass,T targetObject,Map<String,Converter<?>> converters,List<String> noConverts) {
        //Assert.notNull(source,"源拷贝数据对象不能为空！");
        //Assert.notNull(targetClass,"目标拷贝对象class不能为空！");
        Class sourceClass = source.getClass();
        if(!methodCache.containsKey(targetClass)) {
            initClassCache(targetClass);
        }
        if(!methodCache.containsKey(sourceClass)) {
            initClassCache(sourceClass);
        }
        Map<String,Field> targetFields = fieldCache.get(targetClass);
        Map<String,Method> targetMethods = methodCache.get(targetClass);
        if(targetMethods == null) {
            throw new RuntimeException("没有找到可以拷贝的目标字段！");
        }
        Map<String,Method> sourceMethods = methodCache.get(sourceClass);
        if(sourceMethods == null) {
            throw new RuntimeException("没有找到可以拷贝的源数据对象！");
        }
        try {
            if(targetObject == null) {
                if(targetClass.isArray()) {
                    targetObject = (T)Array.newInstance(targetClass,Array.getLength(source));
                } else {
                    targetObject = (T)targetClass.newInstance();
                }
            }

            for(Map.Entry<String,Method> mentry : targetMethods.entrySet()) {
                String methodName = mentry.getKey();
                if(!methodName.startsWith("set")) {
                    continue;
                }
                String fName = methodName.substring(3);
                if(methodName.startsWith("is")) {
                    fName = methodName.substring(2);
                }
                String sourceGetMethodName = "get" + fName;
                // 是否是get方法 或者是is方法
                boolean isGet = sourceMethods.containsKey(sourceGetMethodName);
                Method sourceGetMethod = isGet ? sourceMethods.get(sourceGetMethodName) : sourceMethods.get("is" + fName);
                if(sourceGetMethod == null) {
                    log.warn("Class[{}]中没有找到[{}]方法，所以该字段无法拷贝！",sourceClass,sourceGetMethodName);
                    continue;
                }
                String fieldName = fName.substring(0,1).toLowerCase() + fName.substring(1);
                // 如果该字段不需要转换则跳过
                if(noConverts != null && noConverts.contains(fieldName)) {
                    continue;
                }
                Method targetMethod = mentry.getValue();
                Object sourceValue = sourceGetMethod.invoke(source);
                if(sourceValue == null) {
                    targetMethod.invoke(targetObject,sourceValue);
                    continue;
                }
                // 如果该字段自定义了转换则使用自定义转换
                if(converters != null && converters.containsKey(fieldName)) {
                    targetMethod.invoke(targetObject,converters.get(fieldName).convert(sourceValue));
                    continue;
                }

                Class<?> sourceReturnType = sourceGetMethod.getReturnType();
                Class<?> targetParamType = targetMethod.getParameterTypes()[0];

                Object newSourceValue = null;
                if(sourceValue != null && sourceValue instanceof Collection && !CollectionUtils.isEmpty(targetFields) && targetFields.containsKey(fieldName)) {
                    try {
                        Field targetField = targetFields.get(fieldName);
                        Type targetType = targetField.getGenericType();
                        if(targetType instanceof ParameterizedType) {
                            Class<?> targetTypeClass = (Class<?>)((ParameterizedType)targetType).getActualTypeArguments()[0];
                            if(!primitiveReflect.containsValue(targetTypeClass)) {
                                newSourceValue = (Collection)sourceValue.getClass().newInstance();
                                Collection csourceValue = (Collection)sourceValue;
                                Iterator it = csourceValue.iterator();
                                while(it.hasNext()) {
                                    ((Collection)newSourceValue).add(copyBean(it.next(),targetTypeClass));
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("处理集合泛型时发生异常！",e);
                        newSourceValue = sourceValue;
                    }
                }
                if(newSourceValue == null) {
                    newSourceValue = sourceValue;
                }
                boolean isArray = false;
                if(sourceReturnType.isArray()) {
                    sourceReturnType = sourceReturnType.getComponentType();
                }
                if(targetParamType.isArray()) {
                    isArray = true;
                    targetParamType = targetParamType.getComponentType();
                }

                if(sourceReturnType.isPrimitive()) {
                    sourceReturnType = primitiveReflect.get(sourceReturnType);
                }
                if(targetParamType.isPrimitive()) {
                    targetParamType = primitiveReflect.get(targetParamType);
                }
                // 如果目标拷贝对象与源拷贝对象类型不同，则也去做类似转换
                if(!sourceReturnType.isAssignableFrom(targetParamType)) {
                    targetMethod.invoke(targetObject,copyBean(newSourceValue,targetParamType,null,null));
                    continue;
                }
                // 值强拷贝
                if(isArray) {
                    targetMethod.invoke(targetObject,new Object[]{newSourceValue});
                } else {
                    targetMethod.invoke(targetObject,newSourceValue);
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("数据对象拷贝时发生错误！",e);
            throw new RuntimeException("数据对象拷贝时发生错误！");
        }

        return targetObject;
    }

    /**
     * 初始化初次进行拷贝对象的class进行缓存
     * @param clz
     */
    private synchronized static void initClassCache(Class clz) {
        // 缓存方法
        Method[] methods = clz.getMethods();
        List<Method> methodsList = Arrays.asList(methods);
        // 缓存所有的public的get、set、is方法
        List<Method> needCache = methodsList.stream().filter( m -> (
            (m.getName().startsWith("get")
                || (m.getName().startsWith("is")))
                && m.getParameterCount() == 0)
            || (m.getName().startsWith("set") && m.getParameterCount() == 1)).collect(Collectors.toList());
        if(needCache == null || needCache.size() == 0) {
            return;
        }
        Map<String,Method> methodDetail = new HashMap<>(needCache.size());
        for(Method m : needCache) {
            methodDetail.put(m.getName(),m);

        }
        methodCache.put(clz,methodDetail);

        // 缓存字段
        Field[] fields = clz.getDeclaredFields();
        if(fields == null || fields.length == 0) {
            return;
        }
        Map<String,Field> fieldMap = new HashMap<>(fields.length);
        for(Field field : fields) {
            fieldMap.put(field.getName(),field);
        }
        fieldCache.put(clz,fieldMap);
    }

    /**
     * 自定义转换器
     * @param <F> 转换后的类型
     */
    public interface Converter<F> {

        public F convert(Object source);
    }
}
