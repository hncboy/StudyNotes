//业务处理模块
var fs = require("fs"); //引入文件模块

function readFile() {
	//同步读
	var html = fs.readFileSync("test.html");
	return html;
}

function writeFile() {
	fs.writeFile("test.html", "hello hello nodejs");
	return "write successful";
}

function listCategory() {
	return "a lot of categorys";
}

function listProduct() {
	return "a lot of products";
}

exports.readFile = readFile
exports.writeFile = writeFile
exports.listCategory = listCategory
exports.listProduct = listProduct
