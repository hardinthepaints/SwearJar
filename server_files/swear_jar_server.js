var express = require('express');
var app = express();
var config = require('./config.json');
var PORT=8080;

var braintree = require("braintree");
var gateway = braintree.connect({
  environment: braintree.Environment.Sandbox,
  merchantId: config.braintree.merchantId,
  publicKey: config.braintree.publicKey,
  privateKey: config.braintree.privateKey
});



app.get('/test', function(req, res){
    res.send("This is a test.");
});


// braintree logic
app.get("/client_token", function (req, res) {
  gateway.clientToken.generate({"customerId": "Lf3grewr"}, function (err, response) {
    if(err){
        console.log(err.message);
        console.log(config.publicKey);
    }
    else{
        res.send(response.clientToken);
    }
  });
});

app.post("/checkout", function (req, res) {
  var nonce = req.body.payment_method_nonce;
  var amount = req.body.amount;
    gateway.transaction.sale({
      amount: amount,
      paymentMethodNonce: nonce,
    }, function (err, result) {
        console.log("checkout transaction sale result: " + result);
    });
});




app.set('port', (process.env.PORT || PORT));

var server = app.listen(app.get('port'), function() {


//    var host = server.address().address
    var port = server.address().port
    require('dns').lookup(require('os').hostname(), function (err, add, fam) {
            console.log('App listening at http://%s:%s', add, port);
        })
//    console.log('App listening at http://%s:%s', "172.23.16.99", port)
});
