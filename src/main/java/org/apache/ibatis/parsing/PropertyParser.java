/**
 *    Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class PropertyParser {

  private static final String KEY_PREFIX = "org.apache.ibatis.parsing.PropertyParser.";
  /**
   * The special property key that indicate whether enable a default value on placeholder.
   * <p>
   *   The default value is {@code false} (indicate disable a default value on placeholder)
   *   If you specify the {@code true}, you can specify key and default value on placeholder (e.g. {@code ${db.username:postgres}}).
   * </p>
   * @since 3.4.2
   */
  public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

  /**
   * The special property key that specify a separator for key and default value on placeholder.
   * <p>
   *   The default separator is {@code ":"}.
   * </p>
   * @since 3.4.2
   */
  public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

  private static final String ENABLE_DEFAULT_VALUE = "false";
  private static final String DEFAULT_VALUE_SEPARATOR = ":";

  private PropertyParser() {
    // Prevent Instantiation
  }

  /**
   * 动态替换属性
   * @param string
   * @param variables
   * @return
   */
  public static String parse(String string, Properties variables) {
    //创建VariableTokenHandler
    VariableTokenHandler handler = new VariableTokenHandler(variables);
    //创建GenericTokenParser
    GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
    //调用替换方法
    return parser.parse(string);
  }

  /**
   * 变量token处理器
   */
  private static class VariableTokenHandler implements TokenHandler {
    /**
     * 指定的变量
     */
    private final Properties variables;
    /**
     * 是否支持默认值
     */
    private final boolean enableDefaultValue;
    /**
     * 默认分隔符
     */
    private final String defaultValueSeparator;

    private VariableTokenHandler(Properties variables) {
      this.variables = variables;
      //默认为false，可以指定"org.apache.ibatis.parsing.PropertyParser.enable-default-value"
      this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
      //默认分隔符：，可以指定"org.apache.ibatis.parsing.PropertyParser.default-value-separator"
      this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
    }

    private String getPropertyValue(String key, String defaultValue) {
      return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
    }

    /**
     * 返回替换的变量属性值
     * @param content
     * @return
     */
    @Override
    public String handleToken(String content) {
      //如果变量列表为空
      if (variables != null) {

        String key = content;

        //如果允许使用默认值
        if (enableDefaultValue) {

          //寻找是否包含分隔符
          final int separatorIndex = content.indexOf(defaultValueSeparator);
          String defaultValue = null;

          //如果包含
          if (separatorIndex >= 0) {
            //key为分隔符前的内容
            key = content.substring(0, separatorIndex);
            //默认值为分隔符后的内容
            defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
          }

          //如果默认值不为空，则使用带默认值的参数
          if (defaultValue != null) {
            return variables.getProperty(key, defaultValue);
          }
        }

        //如果变量包含替换值，返回替换值
        if (variables.containsKey(key)) {
          return variables.getProperty(key);
        }
      }
      //兜底，什么都不做
      return "${" + content + "}";
    }
  }

}
