/**
 *    Copyright 2009-2018 the original author or authors.
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
package org.apache.ibatis.jdbc;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Clinton Begin
 */
public class ScriptRunner {

  //配置行隔离符
  private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

  //配置隔离符
  private static final String DEFAULT_DELIMITER = ";";

  //正则判断分隔符
  private static final Pattern DELIMITER_PATTERN = Pattern.compile("^\\s*((--)|(//))?\\s*(//)?\\s*@DELIMITER\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE);

  //连接，不允许修改
  private final Connection connection;

  //报错的时候是否终止
  private boolean stopOnError;
  //报出警告
  private boolean throwWarning;
  //自动提交
  private boolean autoCommit;
  //发送全部脚本
  private boolean sendFullScript;
  //去除\r
  private boolean removeCRs;
  //是否转义
  private boolean escapeProcessing = true;

  //日志输出
  private PrintWriter logWriter = new PrintWriter(System.out);
  //错误日志输出
  private PrintWriter errorLogWriter = new PrintWriter(System.err);

  //分隔符
  private String delimiter = DEFAULT_DELIMITER;

  //去除分隔符后的anything
  private boolean fullLineDelimiter;

  public ScriptRunner(Connection connection) {
    this.connection = connection;
  }

  public void setStopOnError(boolean stopOnError) {
    this.stopOnError = stopOnError;
  }

  public void setThrowWarning(boolean throwWarning) {
    this.throwWarning = throwWarning;
  }

  public void setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  public void setSendFullScript(boolean sendFullScript) {
    this.sendFullScript = sendFullScript;
  }

  public void setRemoveCRs(boolean removeCRs) {
    this.removeCRs = removeCRs;
  }

  /**
   * @since 3.1.1
   */
  public void setEscapeProcessing(boolean escapeProcessing) {
    this.escapeProcessing = escapeProcessing;
  }

  public void setLogWriter(PrintWriter logWriter) {
    this.logWriter = logWriter;
  }

  public void setErrorLogWriter(PrintWriter errorLogWriter) {
    this.errorLogWriter = errorLogWriter;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public void setFullLineDelimiter(boolean fullLineDelimiter) {
    this.fullLineDelimiter = fullLineDelimiter;
  }

  public void runScript(Reader reader) {
    setAutoCommit();

    try {
      if (sendFullScript) {
        executeFullScript(reader);
      } else {
        executeLineByLine(reader);
      }
    } finally {
      rollbackConnection();
    }
  }

  private void executeFullScript(Reader reader) {
    StringBuilder script = new StringBuilder();
    try {
      BufferedReader lineReader = new BufferedReader(reader);
      String line;
      while ((line = lineReader.readLine()) != null) {
        script.append(line);
        script.append(LINE_SEPARATOR);
      }
      String command = script.toString();
      println(command);
      executeStatement(command);
      commitConnection();
    } catch (Exception e) {
      String message = "Error executing: " + script + ".  Cause: " + e;
      printlnError(message);
      throw new RuntimeSqlException(message, e);
    }
  }

  private void executeLineByLine(Reader reader) {
    StringBuilder command = new StringBuilder();
    try {
      BufferedReader lineReader = new BufferedReader(reader);
      String line;
      while ((line = lineReader.readLine()) != null) {
        handleLine(command, line);
      }
      commitConnection();
      checkForMissingLineTerminator(command);
    } catch (Exception e) {
      String message = "Error executing: " + command + ".  Cause: " + e;
      printlnError(message);
      throw new RuntimeSqlException(message, e);
    }
  }

  public void closeConnection() {
    try {
      connection.close();
    } catch (Exception e) {
      // ignore
    }
  }

  /**
   * 设置自动提交
   */
  private void setAutoCommit() {
    try {
      if (autoCommit != connection.getAutoCommit()) {
        connection.setAutoCommit(autoCommit);
      }
    } catch (Throwable t) {
      throw new RuntimeSqlException("Could not set AutoCommit to " + autoCommit + ". Cause: " + t, t);
    }
  }

  private void commitConnection() {
    try {
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    } catch (Throwable t) {
      throw new RuntimeSqlException("Could not commit transaction. Cause: " + t, t);
    }
  }

  /**
   * 如果非自动提交回滚连接
   */
  private void rollbackConnection() {
    try {
      if (!connection.getAutoCommit()) {
        connection.rollback();
      }
    } catch (Throwable t) {
      // ignore
    }
  }

  /**
   * 判断是否有结尾
   * @param command
   */
  private void checkForMissingLineTerminator(StringBuilder command) {
    if (command != null && command.toString().trim().length() > 0) {
      throw new RuntimeSqlException("Line missing end-of-line terminator (" + delimiter + ") => " + command);
    }
  }

  private void handleLine(StringBuilder command, String line) throws SQLException {
    String trimmedLine = line.trim();
    if (lineIsComment(trimmedLine)) {
      Matcher matcher = DELIMITER_PATTERN.matcher(trimmedLine);
      if (matcher.find()) {
        delimiter = matcher.group(5);
      }
      println(trimmedLine);
    } else if (commandReadyToExecute(trimmedLine)) {
      command.append(line.substring(0, line.lastIndexOf(delimiter)));
      command.append(LINE_SEPARATOR);
      println(command);
      executeStatement(command.toString());
      command.setLength(0);
    } else if (trimmedLine.length() > 0) {
      command.append(line);
      command.append(LINE_SEPARATOR);
    }
  }

  private boolean lineIsComment(String trimmedLine) {
    return trimmedLine.startsWith("//") || trimmedLine.startsWith("--");
  }

  private boolean commandReadyToExecute(String trimmedLine) {
    // issue #561 remove anything after the delimiter
    return !fullLineDelimiter && trimmedLine.contains(delimiter) || fullLineDelimiter && trimmedLine.equals(delimiter);
  }

  private void executeStatement(String command) throws SQLException {
    Statement statement = connection.createStatement();
    try {
      statement.setEscapeProcessing(escapeProcessing);
      String sql = command;
      if (removeCRs) {
        sql = sql.replaceAll("\r\n", "\n");
      }
      try {
        boolean hasResults = statement.execute(sql);
        while (!(!hasResults && statement.getUpdateCount() == -1)) {
          checkWarnings(statement);
          printResults(statement, hasResults);
          hasResults = statement.getMoreResults();
        }
      } catch (SQLWarning e) {
        throw e;
      } catch (SQLException e) {
        if (stopOnError) {
          throw e;
        } else {
          String message = "Error executing: " + command + ".  Cause: " + e;
          printlnError(message);
        }
      }
    } finally {
      try {
        statement.close();
      } catch (Exception e) {
        // Ignore to workaround a bug in some connection pools
        // (Does anyone know the details of the bug?)
      }
    }
  }

  private void checkWarnings(Statement statement) throws SQLException {
    if (!throwWarning) {
      return;
    }
    // In Oracle, CREATE PROCEDURE, FUNCTION, etc. returns warning
    // instead of throwing exception if there is compilation error.
    SQLWarning warning = statement.getWarnings();
    if (warning != null) {
      throw warning;
    }
  }

  private void printResults(Statement statement, boolean hasResults) {
    if (!hasResults) {
      return;
    }
    try (ResultSet rs = statement.getResultSet()) {
      ResultSetMetaData md = rs.getMetaData();
      int cols = md.getColumnCount();
      for (int i = 0; i < cols; i++) {
        String name = md.getColumnLabel(i + 1);
        print(name + "\t");
      }
      println("");
      while (rs.next()) {
        for (int i = 0; i < cols; i++) {
          String value = rs.getString(i + 1);
          print(value + "\t");
        }
        println("");
      }
    } catch (SQLException e) {
      printlnError("Error printing results: " + e.getMessage());
    }
  }

  private void print(Object o) {
    if (logWriter != null) {
      logWriter.print(o);
      logWriter.flush();
    }
  }

  private void println(Object o) {
    if (logWriter != null) {
      logWriter.println(o);
      logWriter.flush();
    }
  }

  private void printlnError(Object o) {
    if (errorLogWriter != null) {
      errorLogWriter.println(o);
      errorLogWriter.flush();
    }
  }

}
