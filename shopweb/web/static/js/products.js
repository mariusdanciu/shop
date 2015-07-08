(function() {
    $(function() {

        $.blockUI.defaults.baseZ = 90;

        window.galleryOffset = 0;

        products.refreshList();

        $('#sortSelect, #sortSelect:hidden').on('change', function(evt, params) {
            products.reloadProducts($(this).val());
        });

        $(".close_product_dialog, .close_item_order_dialog").click(function(event) {
            products.closeProductDialog();
            return false;
        });

        products.reloadProducts($('#sortSelect').val());

        
    });

})();

var products = {

    normUrl : function(url, sort) {
        var cat = $.url().param("cat");
        var search = $.url().param("search");

        if (cat === undefined) {
            url += "?search=" + search;
        } else {
            url += "?cat=" + cat;
        }
        url += "&sort=" + sort;
        return url;
    },

    reloadProducts : function(search) {
        $.ajax({
            url : products.normUrl("/products", search),
            cache : false,
            dataType : "html",
            error : function(xhr) {
                common.showError(xhr.statusText);
            },
            success : function(data) {
                $("#item_list").html(data);
                products.refreshList();
                $("#item_list").waitForImages(function() {
                    $("#item_list").fadeIn({
                        duration : 1000
                    });
                });
                return false;
            }
        });

    },

    closeProductDialog : function() {
        window.common.closeDialog();
        setTimeout(function() {
            $("#product_dialog").empty();
            $(".zoomContainer").remove();
        }, 400);
    },

    refreshList : function() {
        $(".item_box").each(function(index) {
            var me = $(this);
            var pid = me.attr("id");

            if (window.admin !== undefined) {
                me.find('.edit_tag_close').click(function(event) {
                    window.admin.deleteProduct(pid);
                    return false;
                });
            }

            me.find('.info_tag_cart').click(function(event) {
                products.closeProductDialog();
                cart.addItem(pid);
                cart.showCart();
                event.stopPropagation();
                return false;
            });

            if (pid !== undefined) {
                me.click(function(event) {
                    var loc = "/productquickview?pid=" + pid;

                    $.ajax({
                        url : loc,
                        cache : false,
                        dataType : "html",
                        error : function(xhr) {
                            common.showError(xhr.statusText);
                        },
                        success : function(data) {
                            $("#product_dialog").html(data);
                            $("#fb-share-button").click(function(e) {
                                FB.ui({
                                    method : 'share',
                                    href : 'http://idid.ro/product?pid=' + pid,
                                }, function(response) {
                                    if (response && !response.error_code) {
                                    } else {
                                    }
                                });
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

                            $('#add_to_cart').click(function(event) {

                                var userOptions = {};
                                $(".custom_option").each(function(i) {
                                    userOptions[$(this).attr("name")] = $(this).val();
                                });
                                $(".custom_text").each(function(i) {
                                    userOptions[$(this).attr("name")] = $(this).val();
                                });

                                cart.addItem(pid, userOptions);
                                cart.showCart();

                                products.closeProductDialog();

                                event.stopPropagation();
                                return false;
                            });

                            var total = $("#gallery ul li").length;
                            if (total > 3) {
                                $('#gallery_right').click(function() {
                                    if (window.galleryOffset * -4 < total * 100) {
                                        window.galleryOffset -= 100;
                                    }
                                    $("#gallery ul").css({
                                        "transform" : "translate(" + window.galleryOffset + "px, 0px)"
                                    });
                                    return false;
                                });

                                $('#gallery_left').click(function() {
                                    if (window.galleryOffset * 4 < 0) {
                                        window.galleryOffset += 100;
                                    }
                                    $("#gallery ul").css({
                                        "transform" : "translate(" + window.galleryOffset + "px, 0px)"
                                    });
                                    return false;
                                });
                            }

                            $('#product_details_tab').tabify();

                            var content = $("#prod_desc").text();
                            $("#prod_desc").html(textile.convert(content));

                            if (window.admin !== undefined) {
                                window.admin.attachToProduct(function() {
                                    products.closeProductDialog();
                                    products.reloadProducts();
                                });
                            }

                            $(".close_product_dialog").click(function(event) {
                                products.closeProductDialog();
                                return false;
                            });

                            $("#product_dialog").waitForImages(function() {
                                $.blockUI({
                                    message : $("#product_dialog"),
                                    css : {
                                        top : '100px',
                                        left : ($(window).width() - 1100) / 2 + 'px',
                                        width : '1100px',
                                        border : 'none',
                                        cursor : null
                                    },
                                    overlayCSS : {
                                        cursor : null,
                                        backgroundColor : '#dddddd'
                                    }
                                });
                            });
                            return false;
                        }
                    });
                    return false;
                });
            } else {
                if (window.admin !== undefined) {
                    window.admin.attachCreateProduct(me);
                }
            }
        });
    }

};
