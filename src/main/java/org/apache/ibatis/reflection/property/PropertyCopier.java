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
package org.apache.ibatis.reflection.property;

import java.lang.reflect.Field;

/**
 * @author Clinton Begin
 *
 * 属性复制器
 */
public final class  PropertyCopier {

  private PropertyCopier() {
    // Prevent Instantiation of Static Class
  }

  /**
   * 将 sourceBean 的属性，复制到 destinationBean 中
   * @param type
   * @param sourceBean
   * @param destinationBean
   */
  public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
    // 循环，从当前类开始，不断复制到父类，直到父类不存在
    Class<?> parent = type;
    while (parent != null) {
      //获取当前类的属性
      final Field[] fields = parent.getDeclaredFields();
      for(Field field : fields) {
        try {
          //设置属性可访问
          field.setAccessible(true);
          //复制属性
          field.set(destinationBean, field.get(sourceBean));
        } catch (Exception e) {
          // Nothing useful to do, will only fail on final fields, which will be ignored.
        }
      }
      //获取父类
      parent = parent.getSuperclass();
    }
  }

}
