<%--
  ~  Licensed to the Apache Software Foundation (ASF) under one
  ~  or more contributor license agreements.  See the NOTICE file
  ~  distributed with this work for additional information
  ~  regarding copyright ownership.  The ASF licenses this file
  ~  to you under the Apache License, Version 2.0 (the
  ~  "License"); you may not use this file except in compliance
  ~  with the License.  You may obtain a copy of the License at
  ~
  ~    https://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing,
  ~  software distributed under the License is distributed on an
  ~  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~  KIND, either express or implied.  See the License for the
  ~  specific language governing permissions and limitations
  ~  under the License.
  --%>
<!DOCTYPE html>
<html>
<head>
    <title>Format Locale Number Test</title>
</head>
<body>
    <h1>Format Locale Number Test</h1>
    <span id="neg-int-no"><g:formatNumber number="${negativeInt}" locale="nb_NO"/></span>
    <span id="neg-long-no"><g:formatNumber number="${negativeLong}" locale="nb_NO" format="#,##0"/></span>
    <span id="neg-decimal-no"><g:formatNumber number="${negativeBigDecimal}" locale="nb_NO" format="#,##0.00"/></span>
    <span id="pos-number-no"><g:formatNumber number="${42}" locale="nb_NO"/></span>
</body>
</html>
