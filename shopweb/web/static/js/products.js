(function() {
    $(function() {

        $.blockUI.defaults.baseZ = 90;

        window.galleryOffset = 0;

        window.opened = {
            small : null,
            large : null
        };

        products.refreshList();

        $('#sortSelect, #sortSelect:hidden').on('change', function(evt, params) {
            products.reloadProducts($(this).val());
        });

        $(".close_item_order_dialog").click(function(event) {
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
                $("#item_list").fadeIn({
                    duration : 1000
                });
                return false;
            }
        });

    },

    closeProductDialog : function() {
        window.common.closeDialog();
    },

    scrollToProd : function() {
        $("body, html").animate({
            scrollTop : $('.product_detail_border').offset().top - 150
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

                            if (window.opened.small)
                                window.opened.small.show();
                            if (window.opened.large)
                                window.opened.large.remove();

                            var detail = $(data);
                            var div = $("<div class='product_detail_border'></div>").append(detail);
                            var li = $("<li></li>").append(div);

                            me.parent().hide();
                            me.parent().after(li);

                            window.opened.small = me.parent();
                            window.opened.large = li;

                            var total = detail.find("#gallery ul li").length;
                            if (total > 3) {
                                detail.find('#gallery_right').click(function() {
                                    if (window.galleryOffset * -4 < total * 100) {
                                        window.galleryOffset -= 100;
                                    }
                                    detail.find("#gallery ul").css({
                                        "transform" : "translate(" + window.galleryOffset + "px, 0px)"
                                    });
                                    return false;
                                });

                                detail.find('#gallery_left').click(function() {
                                    if (window.galleryOffset * 4 < 0) {
                                        window.galleryOffset += 100;
                                    }
                                    detail.find("#gallery ul").css({
                                        "transform" : "translate(" + window.galleryOffset + "px, 0px)"
                                    });
                                    return false;
                                });
                            }

                            detail.find('#add_to_cart').click(function(event) {

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

                            detail.find("#sel_img").elevateZoom({
                                gallery : 'detail_box',
                                cursor : 'pointer',
                                galleryActiveClass : 'active',
                                imageCrossfade : true,
                                loadingIcon : '/images/ajax-loader.gif',
                                scrollZoom : true,
                                borderSize : 1
                            });

                            detail.find("#fb-share-button").click(function(e) {
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

                            detail.find(".close_product_dialog").click(function(event) {

                                window.opened.small = null;
                                window.opened.large = null;

                                me.parent().show();
                                li.remove();
                                return false;
                            });

                            var tab = detail.find('#product_details_tab');
                            tab.tabify();

                            detail.find('#product_details_tab li').click(function(e, data) {
                                window.setTimeout(function() {
                                    products.scrollToProd();
                                }, 100);
                            });

                            var content = detail.find("#prod_desc").text();
                            detail.find("#prod_desc").html(textile.convert(content));

                            if (window.admin !== undefined) {
                                window.admin.attachToProduct(detail, function() {
                                    me.parent().show();
                                    li.remove();
                                    products.reloadProducts();
                                });
                            }

                            products.scrollToProd();
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
