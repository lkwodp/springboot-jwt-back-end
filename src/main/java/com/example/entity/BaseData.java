package com.example.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * @author blbyd_li
 * @data 2023/9/10
 * @apiNote
 */
public interface BaseData {

    /**
     * 在生成对象（View Object）的基础上，应用一个自定义操作（通过 Consumer 函数式接口实现）
     * @param clazz 传入类
     * @param consumer  Consumer 函数式
     * @return 新构建的对象
     * @param <V> 泛型
     */
    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer){
        //获取新的对象
        V v = this.asViewObject(clazz);
        //添加 Consumer 函数式
        consumer.accept(v);
        return v;
    }

    /**
     * 根据传入的类（clazz）生成一个新的对象，并将当前对象（this）的字段值复制到新对象中的相应字段
     * @param clazz 传入类
     * @return 新构造的对象
     * @param <V> 泛型
     */
    default <V> V asViewObject(Class<V> clazz){
        try{
            //通过反射获取传入类（clazz）中声明的所有字段（declaredFields）
            Field[] declaredFields = clazz.getDeclaredFields();
            //获取传入类（clazz）的默认构造函数（constructor）
            Constructor<V> constructor = clazz.getConstructor();
            //使用默认构造函数（constructor）创建传入类（clazz）的新实例（v）
            V v = constructor.newInstance();
            //遍历当前对象（this）的字段，调用 convert 方法将当前对象的字段值复制到新对象（v）的相应字段
            for (Field declaredField : declaredFields) this.convert(declaredField,v);
            return v;
        }catch (ReflectiveOperationException e){
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 当前对象（this）中指定名称的字段（Field）的值复制给传入的另一个对象（vo）的相应字段
     * @param field 字段名
     * @param vo 目标对象
     */
    private void convert(Field field,Object vo){
        try {
            //通过反射获取当前对象（this）中与传入的字段（field）具有相同名称的字段（source）
            Field source = this.getClass().getDeclaredField(field.getName());
            //设置字段的可访问性，将传入的字段（field）和获取到的字段（source）设置为可访问
            field.setAccessible(true);
            source.setAccessible(true);
            //使用反射将当前对象（this）中的字段（source）的值设置给传入的对象（vo）中的字段（field）
            field.set(vo,source.get(this));
        }catch (IllegalAccessException  | NoSuchFieldException ignored){}
    }
}
