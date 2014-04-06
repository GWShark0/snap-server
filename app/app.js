var express = require("express");
var app = express();

var exec = require('child_process').exec;

// include the express body parser
app.configure(function () {
    //app.use(express.bodyParser());
    app.use(express.bodyParser({ keepExtensions: true, uploadDir: __dirname + '/uploads/images' }));
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

var users = {

    'alice': { snaps: [{from: 'alex', url: 'blah.com'}]},
    'alex': { snaps: []},
    'greg': { snaps: []},
    'ilya': { snaps: []}
};

// include the node file module
var fs = require('fs');

var IMAGES_DIR = "/uploads/images/";
var VIDEOS_DIR = "/uploads/videos/";

// post some files
app.post('/upload', function(req, res) {


    fs.readFile(req.files.file.path, function (err, data) {




        var imageName = req.files.file.name;

        var from_user = "greg";
        var to_user = "alice";

        // if there's an error
        if (!imageName) {
            console.log("There was an error");
            res.redirect("/");
            res.end();
        } else {

            var imagePath = __dirname + IMAGES_DIR + imageName;
            var videoName = imageName.replace(/\..+$/, '.mp4');
            var videoPath = __dirname + VIDEOS_DIR + videoName;

            // let's see it
            fs.writeFile(imagePath, data, function (err) {


                exec('java -jar SplittyFlickerServer-0.0.1-SNAPSHOT.jar ' + imagePath + ' ' + videoPath,
                    function callback(error, stdout, stderr){

                    console.log(stdout);
                    console.log(stderr);

                        console.log("IMAGE_PATH: " + imagePath);
                        console.log("VIDEO_PATH: " + videoPath);

                        users[to_user]['snaps'].push({from: from_user, url: VIDEOS_DIR + videoName});
                        console.log("Sent: " + VIDEOS_DIR + videoName +
                            " From: " + from_user +
                            " To: " + to_user);

                    // res.redirect(VIDEOS_DIR + videoName);
                    res.end('success');
                });
            });
        }
    });
});

//app.get('/users/:name', function (req, res){
//    res.writeHead(200, {"Content-Type": "application/json"});
//
//    var user = users[req.params.name];
//
//    console.log(user);
//
//    var snaps = user['snaps'];
//
//    if (user == undefined) {
//        res.end("{'error':'not a user'}");
//    } else {
//        res.end(JSON.stringify(snaps));
//    }
//});

// show some images
app.get(IMAGES_DIR + ':file', function (req, res){
    var file = req.params.file;
    var img = fs.readFileSync(__dirname + IMAGES_DIR + file);
    res.writeHead(200, {'Content-Type': 'image/jpg' });
    res.end(img, 'binary');
});

// show some videos
app.get(VIDEOS_DIR + ':file', function (req, res){
    var file = req.params.file;
    var img = fs.readFileSync(__dirname + VIDEOS_DIR + file);
    res.writeHead(200, {'Content-Type': 'video/mp4' });
    res.end(img, 'binary');
});

console.log('Server listening on port 8080.');
app.listen(8080);