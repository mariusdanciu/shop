(function() {
    $(function() {
        var galleryOffset = 0;
        var total = $("#gallery ul li").length;


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
        
        $("#del_prod").click(function(event) {
            var pid = $.url().param("pid");
            window.admin.deleteProduct(pid, function() {
            	window.location.href = "/";
            });
            event.stopPropagation();
            event.preventDefault();
            return false;
        });

    });

})();