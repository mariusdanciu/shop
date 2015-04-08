(function() {
    $(function() {
        var galleryOffset = 0;
        var total = $("#gallery ul li").length;
        if (total > 3) {
            $('#gallery_right').click(function() {
                if (galleryOffset * -4 < total * 100) {
                    galleryOffset -= 100;
                }
                $("#gallery ul").css({
                    "transform" : "translate(" + galleryOffset + "px, 0px)"
                });
                return false;
            });

            $('#gallery_left').click(function() {
                if (galleryOffset * 4 < 0) {
                    galleryOffset += 100;
                }
                $("#gallery ul").css({
                    "transform" : "translate(" + galleryOffset + "px, 0px)"
                });
                return false;
            });
        }

        $('#product_details_tab').tabify();

        $(".close_item_order_dialog").click(function(event) {
            common.closeDialog();
            return false;
        });

        $("#add_to_cart").click(function(event) {
            var pid = $.url().param("pid");
            var userOptions = {};
            $(".custom_option").each(function(i) {
                userOptions[$(this).attr("name")] = $(this).val();
            });
            $(".custom_text").each(function(i) {
                userOptions[$(this).attr("name")] = $(this).val();
            });

            common.closeDialog();
            cart.addItem(pid, userOptions);
            cart.showCart();
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

        var content = $("#prod_desc").text();
        $("#prod_desc").html(textile.convert(content));

    });

})();