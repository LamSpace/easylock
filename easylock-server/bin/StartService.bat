@echo off

rem   Copyright 2021 the original author, Lam Tong

rem   Licensed under the Apache License, Version 2.0 (the "License");
rem   you may not use this file except in compliance with the License.
rem   You may obtain a copy of the License at

rem       http://www.apache.org/licenses/LICENSE-2.0

rem   Unless required by applicable law or agreed to in writing, software
rem   distributed under the License is distributed on an "AS IS" BASIS,
rem   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem   See the License for the specific language governing permissions and
rem   limitations under the License.

setlocal

set lib=.;../lib/easylock-common-1.3.2.jar;../lib/netty-all-4.1.6.Final.jar;../lib/protobuf-java-3.18.0.jar;

set conf=../conf

set application=io.github.lamtong.easylock.server.EasyLockApplication

set serverPort=40417

set serverBacklog=1024

java -cp "%lib%" -Xbootclasspath/a:"%conf%" "%application%" "--easy-lock.server.port=%serverPort%" "--easy-lock.server.backlog=%serverBacklog%"

endlocal