/*
 	在node.js中，所谓的模块，就是别人写的 js
	比如在前面教程中的 server.js 里引入 http模块
 */

var http = require('http')
//通过./hncboy加载这个模块。 记得，比如加上 ./ ,否则会到 node安装目录下去寻找
var hncboy = require('./hncboy') 
hncboy.hi() //调用hi函数
var server = http.createServer(hncboy.service) //基于hncboy.service() 函数创建服务
server.listen(8088)
