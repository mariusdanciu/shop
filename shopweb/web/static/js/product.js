(function() {
    $(function() {
        var galleryOffset = 0;
        var total = $("#gallery ul li").length;

        if (window.admin) {
        }

        $("#add_to_cart_box").click(function(event) {
            var pid = $.url().param("pid");
            cart.addItem(pid);
            event.stopPropagation();
            return false;
        });

        $("#sel_img").elevateZoom({
            gallery : 'detail_box',
            cursor : 'pointer',
            galleryActiveClass : 'active',
            imageCrossfade : true,
            loadingIcon : '/images/ajax-loader.gif',
            scrollZoom : true,
            borderSize : 1
        });

        $("#fb-share-button").click(function(e) {
            var pid = $.url().param("pid");
            FB.ui({
                method : 'share',
                href : 'http://idid.ro/product?pid=' + pid,
            }, function(response) {
                if (response && !response.error_code) {
                } else {
                }
            });
        });

    });

})();