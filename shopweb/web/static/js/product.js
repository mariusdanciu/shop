(function() {
  $(function() {

    $('#product_details_tab').tabify();

    $("#add_to_cart").click(function(event) {
      var pid = $.url().param("pid");
      cart.addItem(pid);
      cart.showCart();
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
        href : 'http://localhost:8887/product?pid=' + pid,
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