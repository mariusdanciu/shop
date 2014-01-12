(function() {
  $(function() {

    $("#add_to_cart").click(function(event) {
      var pid = $.url().param("pid");
      cart.addItem(pid);
      $('#cart_popup').show();
      event.stopPropagation();
    });
    
    $(".small_img").click(function(event) {
      zoomFunc($(event.target));
      event.stopPropagation();
    });
    
    var zoomFunc = function(el){
      var src = $(el).attr("src");
      $(".sel_img").attr("src", src);
      
      var sub = src.substring(0, src.lastIndexOf("."));
      var ext = src.substring(src.lastIndexOf("."));
      var large = sub + "-large" + ext;

      $('.detail_box').zoom({url: large});
    }
    
    $('.detail_box').zoom({ url : zoomFunc($('.sel_img')) });
    
  });
  
})();