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
package org.apache.ibatis.session;

/**
 * 一级缓存的范围
 * STATEMENT，同一个SqlSession，从数据库查询完后会放入缓存，然后立马会清除一级缓存，EXECUTOR的LocalCache就会清空一级缓存
 * SESSION，同一个Session的查询，会被放入到一级缓存，再次查询时，将可能从一级缓存中获取，或者二级缓存（也就是说不会清空一级缓存）
 * @author Eduardo Macarron
 */
public enum LocalCacheScope {
  SESSION,STATEMENT
}
