(function(open) {
XMLHttpRequest.prototype.open = function(method,uri, async, user, pass) {
        XMLHttpRequest.prototype.tempUrl=uri;
        return open.apply(this, arguments);
};
})(XMLHttpRequest.prototype.open);


(function(send) {
XMLHttpRequest.prototype.send = function(data) {
         if(this.tempUrl.indexOf('localhost')===-1){ //check if the url is directly to geoserver or through wso2
                this.setRequestHeader('Authorization', auth_token_wso2);
         }
        send.call(this, data);
};
})(XMLHttpRequest.prototype.send)
