/**
 *    Copyright 2009-2017 the original author or authors.
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

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

  /**
   * 打开token
   */
  private final String openToken;

  /**
   * 关闭token
   */
  private final String closeToken;

  /**
   * token 处理器
   */
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  /**
   * token解析
   * @param text
   * @return
   */
  public String parse(String text) {
    //如果是空返回""
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    // 如果不包含打开标志，不替换
    int start = text.indexOf(openToken, 0);
    if (start == -1) {
      return text;
    }

    // 否则将text转化为数组
    char[] src = text.toCharArray();
    //偏移量
    int offset = 0;
    //返回值
    final StringBuilder builder = new StringBuilder();
    //表达式
    StringBuilder expression = null;

    //如果有多个，循环替换
    while (start > -1) {
      //如果包含转移字符
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
        // 识别转义标志，不要误判
        builder.append(src, offset, start - offset - 1).append(openToken);
        // 偏移量+打开标志长度
        offset = start + openToken.length();
      } else {
        // found open token. let's search close token.
        // 有打开标志，寻找关闭标志

        // 初始化表达式
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        // 将不需要替换的字符原值复制
        builder.append(src, offset, start - offset);
        // 偏移量+打开标志长度
        offset = start + openToken.length();

        // 寻找关闭标志
        int end = text.indexOf(closeToken, offset);

        // 如果存在结束标志，则清除转义字符
        while (end > -1) {
          //去除转移字符
          if (end > offset && src[end - 1] == '\\') {
            // this close token is escaped. remove the backslash and continue.
            // 表达式添加变量字符
            expression.append(src, offset, end - offset - 1).append(closeToken);
            // 偏移量+关闭标志长度
            offset = end + closeToken.length();
            // 重新寻找结束标识
            end = text.indexOf(closeToken, offset);
          } else {
            // 表达式添加
            expression.append(src, offset, end - offset);
            // 偏移量+关闭标志长度
            offset = end + closeToken.length();
            break;
          }
        }
        //如果关闭标志不存在
        if (end == -1) {
          // close token was not found.
          // 没有找到结束标志，直接添加
          builder.append(src, start, src.length - start);
          //标志为设置到末尾
          offset = src.length;
        } else {
          //否则添加替换的值
          builder.append(handler.handleToken(expression.toString()));
          //标志为设置为结束位置+关闭标志
          offset = end + closeToken.length();
        }
      }
      //循环寻找开始标志
      start = text.indexOf(openToken, offset);
    }


    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
