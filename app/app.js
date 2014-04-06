var express = require("express");
var app = express();

// include the express body parser
app.configure(function () {
  app.use(express.bodyParser());
});

var form = "<!DOCTYPE HTML><html><body>" +
"<form method='post' action='/upload' enctype='multipart/form-data'>" +
"<input type='file' name='image'/>" +
"<input type='submit' /></form>" +
"</body></html>";

app.get('/', function (req, res){
	res.writeHead(200, {'Content-Type': 'text/html' });
	res.end(form);	
});

// include the node file module
var fs = require('fs');

var IMAGES_DIR = "/uploads/images/";
var VIDEOS_DIR = "/uploads/videos/";

// post some files
app.post('/upload', function(req, res) {

	fs.readFile(req.files.image.path, function (err, data) {

		var imageName = req.files.image.name

		// if there's an error
		if (!imageName) {
			console.log("There was an error")
			res.redirect("/");
			res.end();
		} else {

			var newPath = __dirname + IMAGES_DIR + imageName;
            var videoName = imageName.replace(/\..+$/, '.mp4');

            // let's see it
			fs.writeFile(newPath, data, function (err) {

				res.redirect(VIDEOS_DIR + videoName);
			});
		}
	});
});

// show some images
app.get(IMAGES_DIR + ':file', function (req, res){
	var file = req.params.file;
	var img = fs.readFileSync(__dirname + IMAGES_DIR + file);
	res.writeHead(200, {'Content-Type': 'image/jpg' });
	res.end(img, 'binary');
});

// show some videos
app.get(VIDEOS_DIR + '/uploads/videos/:file', function (req, res){
	var file = req.params.file;
	var img = fs.readFileSync(__dirname + VIDEOS_DIR + file);
	res.writeHead(200, {'Content-Type': 'video/mp4' });
	res.end(img, 'binary');
});

console.log('Server listening on port 8080.');
app.listen(8080);