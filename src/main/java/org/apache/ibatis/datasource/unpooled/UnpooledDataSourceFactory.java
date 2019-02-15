/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.datasource.unpooled;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * @author Clinton Begin
 *
 * 非池化的数据源
 */
public class UnpooledDataSourceFactory implements DataSourceFactory {

  /**
   * 驱动属性前缀
   */
  private static final String DRIVER_PROPERTY_PREFIX = "driver.";

  /**
   * 前缀长度
   */
  private static final int DRIVER_PROPERTY_PREFIX_LENGTH = DRIVER_PROPERTY_PREFIX.length();

  /**
   * 数据源
   */
  protected DataSource dataSource;

  /**
   * 构造函数
   */
  public UnpooledDataSourceFactory() {
    this.dataSource = new UnpooledDataSource();
  }

  /**
   * 设置属性
   * @param properties
   */
  @Override
  public void setProperties(Properties properties) {
    // 驱动属性
    Properties driverProperties = new Properties();
    // 创建MetaObject
    MetaObject metaDataSource = SystemMetaObject.forObject(dataSource);

    // 循环设置属性
    for (Object key : properties.keySet()) {
      //获取属性名称
      String propertyName = (String) key;

      //如果是驱动
      if (propertyName.startsWith(DRIVER_PROPERTY_PREFIX)) {

        //获取属性值
        String value = properties.getProperty(propertyName);

        //设置属性
        driverProperties.setProperty(propertyName.substring(DRIVER_PROPERTY_PREFIX_LENGTH), value);
        //如果有set方法
      } else if (metaDataSource.hasSetter(propertyName)) {
        //获取属性值
        String value = (String) properties.get(propertyName);
        //获取转化后的类型
        Object convertedValue = convertValue(metaDataSource, propertyName, value);
        //设置属性
        metaDataSource.setValue(propertyName, convertedValue);
      } else {
        throw new DataSourceException("Unknown DataSource property: " + propertyName);
      }
    }
    //如果驱动属性存在，则赋值
    if (driverProperties.size() > 0) {
      metaDataSource.setValue("driverProperties", driverProperties);
    }
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * 类型转化
   * @param metaDataSource
   * @param propertyName
   * @param value
   * @return
   */
  private Object convertValue(MetaObject metaDataSource, String propertyName, String value) {
    Object convertedValue = value;
    Class<?> targetType = metaDataSource.getSetterType(propertyName);
    if (targetType == Integer.class || targetType == int.class) {
      convertedValue = Integer.valueOf(value);
    } else if (targetType == Long.class || targetType == long.class) {
      convertedValue = Long.valueOf(value);
    } else if (targetType == Boolean.class || targetType == boolean.class) {
      convertedValue = Boolean.valueOf(value);
    }
    return convertedValue;
  }

}
