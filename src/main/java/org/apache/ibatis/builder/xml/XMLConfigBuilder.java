/*
 *    Copyright 2009-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * 帮助解析mybatis配置文件 config.xml,Configuration构建器
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLConfigBuilder extends BaseBuilder {

  private boolean parsed;
  private final XPathParser parser;
  private String environment;
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(Configuration.class, reader, environment, props);
  }

  public XMLConfigBuilder(Class<? extends Configuration> configClass, Reader reader, String environment, Properties props) {
    this(configClass, new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(Configuration.class, inputStream, environment, props);
  }

  public XMLConfigBuilder(Class<? extends Configuration> configClass, InputStream inputStream, String environment, Properties props) {
    this(configClass, new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  private XMLConfigBuilder(Class<? extends Configuration> configClass, XPathParser parser, String environment, Properties props) {
    super(newConfig(configClass)); //创建一个Configuration
    ErrorContext.instance().resource("SQL Mapper Configuration");
    this.configuration.setVariables(props);
    this.parsed = false;
    this.environment = environment;
    this.parser = parser;
  }

  public Configuration parse() { //将mybatis config xml 解析为 Configuration
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration; //至此解析完所有的Mapper.class mapper.xml mybatisConfig.xml
  }
  //正式开始解析Mybatis config xml
  private void parseConfiguration(XNode root) { //这是一个指挥者方法，自己作为建造者，也作为指挥者
    try {
      // issue #117 read properties first
      propertiesElement(root.evalNode("properties")); //开始解析<properties>标签
      Properties settings = settingsAsProperties(root.evalNode("settings")); //解析<settings>
      loadCustomVfs(settings); //VFS是啥
      loadCustomLogImpl(settings); //加载日志实现类 使用别名或者类的全限定名
      typeAliasesElement(root.evalNode("typeAliases")); //注册类别名
      pluginElement(root.evalNode("plugins")); //插件
      objectFactoryElement(root.evalNode("objectFactory"));
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      settingsElement(settings); //将<settings>设置到Configuration中
      // read it after objectFactory and objectWrapperFactory issue #631
      environmentsElement(root.evalNode("environments"));
      databaseIdProviderElement(root.evalNode("databaseIdProvider")); //数据库名称提供者（单纯的给dataSource提供一个名字）
      typeHandlerElement(root.evalNode("typeHandlers"));//注册JDBC与JAVA类型之间转换的处理器
      mapperElement(root.evalNode("mappers")); //加载Mapper.xml，这是最复杂的
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  private Properties settingsAsProperties(XNode context) {
    if (context == null) {
      return new Properties();
    }
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) { //检查configuration.class是否有对应属性的setter方法
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  private void loadCustomVfs(Properties props) throws ClassNotFoundException {
    String value = props.getProperty("vfsImpl");
    if (value != null) {
      String[] clazzes = value.split(",");
      for (String clazz : clazzes) {
        if (!clazz.isEmpty()) {
          @SuppressWarnings("unchecked")
          Class<? extends VFS> vfsImpl = (Class<? extends VFS>)Resources.classForName(clazz);
          configuration.setVfsImpl(vfsImpl);
        }
      }
    }
  }

  private void loadCustomLogImpl(Properties props) {
    Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
    configuration.setLogImpl(logImpl);
  }

  private void typeAliasesElement(XNode parent) {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String typeAliasPackage = child.getStringAttribute("name");
          configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
        } else {
          String alias = child.getStringAttribute("alias");
          String type = child.getStringAttribute("type");
          try {
            Class<?> clazz = Resources.classForName(type);
            if (alias == null) {
              typeAliasRegistry.registerAlias(clazz);
            } else {
              typeAliasRegistry.registerAlias(alias, clazz);
            }
          } catch (ClassNotFoundException e) {
            throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
          }
        }
      }
    }
  }

  private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        String interceptor = child.getStringAttribute("interceptor");
        Properties properties = child.getChildrenAsProperties();
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).getDeclaredConstructor().newInstance();
        interceptorInstance.setProperties(properties);
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  private void objectFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties properties = context.getChildrenAsProperties();
      ObjectFactory factory = (ObjectFactory) resolveClass(type).getDeclaredConstructor().newInstance();
      factory.setProperties(properties);
      configuration.setObjectFactory(factory);
    }
  }

  private void objectWrapperFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).getDeclaredConstructor().newInstance();
      configuration.setObjectWrapperFactory(factory);
    }
  }

  private void reflectorFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ReflectorFactory factory = (ReflectorFactory) resolveClass(type).getDeclaredConstructor().newInstance();
      configuration.setReflectorFactory(factory);
    }
  }
  //解析mybatis 配置 <proerties>标签
  private void propertiesElement(XNode context) throws Exception {
    if (context != null) {
      Properties defaults = context.getChildrenAsProperties();
      String resource = context.getStringAttribute("resource"); //<properties resource="" url="">是外部properties
      String url = context.getStringAttribute("url");
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }
      if (resource != null) {
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) {
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      Properties vars = configuration.getVariables(); //作为方法参数传入的properties
      if (vars != null) {
        defaults.putAll(vars);
      }
      parser.setVariables(defaults); //保存解析完的properties
      configuration.setVariables(defaults); //保存解析完的properties
    }
  }

  private void settingsElement(Properties props) {
    configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));//默认的自动映射行为为PARTIAL（不映射嵌套属性）
    configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));//映射时遇到未知列的行为，默认为忽略
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true)); //缓存开关，默认true开启
    configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));//proxyFactory，默认的为JavassistProxyFactory
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));//延迟加载开关，为true时，所有嵌套属性都会延迟加载
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));//为true时，调用对象的任意方法都会触发加载属性，fasle只会按需加载
    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));//是否允许单个语句返回多个结果集
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));//使用列列标签代替列名，就是是否使用as的别名
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));//允许JDBC支持自动生成主键，为true的话，将强制使用生成主键
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));//配置默认的执行器，REUSE会重用PreparedStatement
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));//设置超时时间，等待数据库查询响应的时间
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));//为驱动的结果集获取的数量设置一个建议值
    configuration.setDefaultResultSetType(resolveResultSetType(props.getProperty("defaultResultSetType")));//指定语句的默认滚动策略
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));//是否开启驼峰命名自动映射，从数据库列名A_COLUMN，到java属性名aColumn
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));//是否允许在嵌套语句中使用分页
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));//SESSION:缓存一个会话的所有查询
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));//当没有为参数指定JDBC类型的时，空值的默认的JDBC类型
    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));//指定对象的那些方法触发一次延迟加载
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true)); //是否允许在嵌套语句中使用结果处理器，如果允许设置为false
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));//指定动态sql生成的脚本语言
    configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));//指定Enum使用的默认处理器
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));//当结果集为NULL时，是否调用对象的setter方法
    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));//允许使用方法参数名作为语句参数名，就是说使用方法参数名，作为占位符的映射${方法参数名}
    configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));//当返回行的所有列都是空时，返回NULL；为true时返回一个属性为空的属性
    configuration.setLogPrefix(props.getProperty("logPrefix")); //指定Mybatis的日志名前缀
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));//指定一个提供Configuration实例的类，用来加载被反序列化对象的延迟加载属性值
    configuration.setShrinkWhitespacesInSql(booleanValueOf(props.getProperty("shrinkWhitespacesInSql"), false));//从sql中删除多余的空格字符，这会影响SQL的文字字符串
    configuration.setArgNameBasedConstructorAutoMapping(booleanValueOf(props.getProperty("argNameBasedConstructorAutoMapping"), false));//当应用构造器自动映射时，参数名称被用来搜索要映射的列，而不是依赖列的顺序
    configuration.setDefaultSqlProviderType(resolveClass(props.getProperty("defaultSqlProviderType")));//指定一个拥有provider方法的sql provider类
    configuration.setNullableOnForEach(booleanValueOf(props.getProperty("nullableOnForEach"), false));//为'foreach'标签的'nullable'属性指定默认属性
  }

  private void environmentsElement(XNode context) throws Exception {
    if (context != null) {
      if (environment == null) {
        environment = context.getStringAttribute("default");
      }
      for (XNode child : context.getChildren()) {
        String id = child.getStringAttribute("id"); //<environment id="development">
        if (isSpecifiedEnvironment(id)) { //找到default环境 没有激活的环境根本不会加载
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager")); //创建事务管理器
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource")); //<dataSource>标签
          DataSource dataSource = dsFactory.getDataSource();
          Environment.Builder environmentBuilder = new Environment.Builder(id)
              .transactionFactory(txFactory)
              .dataSource(dataSource);
          configuration.setEnvironment(environmentBuilder.build());
          break;
        }
      }
    }
  }

  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {
      String type = context.getStringAttribute("type");
      // awful patch to keep backward compatibility
      if ("VENDOR".equals(type)) {
        type = "DB_VENDOR";
      }
      Properties properties = context.getChildrenAsProperties();
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).getDeclaredConstructor().newInstance();
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    if (environment != null && databaseIdProvider != null) {
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      configuration.setDatabaseId(databaseId);
    }
  }

  private TransactionFactory transactionManagerElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      TransactionFactory factory = (TransactionFactory) resolveClass(type).getDeclaredConstructor().newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  private DataSourceFactory dataSourceElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");//连接池的类型 POOLED 就是内置的 缓存的连接池  UNPOOLED就是不重用的连接池 ，指定类的全限定名，就会创建指定的线程池工厂，需要自己实现DataSourceFactory，返回自己的dataSource
      Properties props = context.getChildrenAsProperties();
      DataSourceFactory factory = (DataSourceFactory) resolveClass(type).getDeclaredConstructor().newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  private void typeHandlerElement(XNode parent) {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String typeHandlerPackage = child.getStringAttribute("name");
          typeHandlerRegistry.register(typeHandlerPackage); //注解式声明类型处理器
        } else {
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            typeHandlerRegistry.register(typeHandlerClass); //typeHandlerRegistry是Configuration同一个引用
          }
        }
      }
    }
  }
  //解析mybatisConfig.xml的<mappers></mappers>标签
  //Mapper.class Mapper.xml 无论是谁先加载，都会加载另外一个
  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) { //遍历所有的Mapper.xml 或者Mapper.class
        if ("package".equals(child.getName())) { //包扫描 （是扫描类）扫描同路径下面的mapper.xml mapper.class
          String mapperPackage = child.getStringAttribute("name");
          configuration.addMappers(mapperPackage);
        } else {
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);
            try(InputStream inputStream = Resources.getResourceAsStream(resource)) {
              XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
              mapperParser.parse();
            }
          } else if (resource == null && url != null && mapperClass == null) {
            ErrorContext.instance().resource(url);
            try(InputStream inputStream = Resources.getUrlAsStream(url)){
              XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
              mapperParser.parse();
            }
          } else if (resource == null && url == null && mapperClass != null) {
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface); //加载Mapper.class 也会加载同名同路径的Mapper.xml
          } else { //<mapper></mapper> 只能有resource，url，mapperClass只能被一个指定
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }
  //是否是指定的Environment
  private boolean isSpecifiedEnvironment(String id) {
    if (environment == null) {
      throw new BuilderException("No environment specified.");
    }
    if (id == null) {
      throw new BuilderException("Environment requires an id attribute.");
    }
    return environment.equals(id);
  }

  private static Configuration newConfig(Class<? extends Configuration> configClass) {
    try {
      return configClass.getDeclaredConstructor().newInstance();
    } catch (Exception ex) {
      throw new BuilderException("Failed to create a new Configuration instance.", ex);
    }
  }

}
