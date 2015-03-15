(function() {
  $(function() {

    $.blockUI.defaults.baseZ = 90;

    products.refreshList();

    $("#sortSelect").chosen({
      "disable_search" : true
    });

    $('#sortSelect').on('change', function(evt, params) {
      $("#item_list").load(products.normUrl("/products", $(this).val()), function(response, status, xhr) {
        if (status == "error") {
          $("#notice_connect_e").show().delay(5000).fadeOut("slow");
        } else {
          products.refreshList();
        }
      });
    })

    $(".close_product_dialog").click(function(event) {
      products.closeProductDialog();
      return false;
    });

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

  reloadProducts : function() {
    $("#item_list").load(products.normUrl("/products", $('#sortSelect').val()), function(response, status, xhr) {
      if (status == "error") {
        $("#notice_connect_e").show().delay(5000).fadeOut("slow");
      } else {
        products.refreshList();
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
        cart.addItem(pid);
        cart.showCart();
        event.stopPropagation();
        return false;
      });

      if (pid !== undefined) {
        me.click(function(event) {
          var loc = "/productquickview?pid=" + pid;
          $("#product_dialog").load(loc, function(response, status, xhr) {
            if (status == "error") {
              $("#notice_connect_e").show().delay(5000).fadeOut("slow");
            } else {

              $("#fb-share-button").click(function(e) {
                FB.ui({
                  method : 'share',
                  href : 'http://localhost:8887/product?pid=' + pid,
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
                products.closeProductDialog();
                cart.addItem(pid);
                cart.showCart();
                event.stopPropagation();
                return false;
              });

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
