(function() {
  $(function() {

    $('#product_details_tab').tabify();

    $(".close_item_order_dialog").click(function(event) {
        common.closeDialog();
        return false;
      });
    
    $("#add_to_cart").click(function(event) {
      var pid = $.url().param("pid");
      
	  $('#order_product').unbind().click(function(event){
		  var text = $('#product_comments').val();
		  common.closeDialog();
	      cart.addItem(pid, text);
	      cart.showCart();
	      event.stopPropagation(); 	
	      return false;
	  });
	  
      $.blockUI({
          message : $("#item_order_dialog"),
          css : {
            top : '100px',
            left : ($(window).width() - 400) / 2 + 'px',
            width : '400px',
            border : 'none',
            cursor : null
          },
          overlayCSS : {
            cursor : null,
            backgroundColor : '#dddddd'
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