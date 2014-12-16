(function() {
	$(function() {

        $('#product_details_tab').tabify();

		$("#add_to_cart").click(function(event) {
			var pid = $.url().param("pid");
			cart.addItem(pid);
			cart.showCart();
			event.stopPropagation();
		});

		$("#sel_img").elevateZoom({
			gallery : 'detail_box',
			cursor : 'pointer',
			galleryActiveClass : 'active',
			imageCrossfade : true,
			loadingIcon : '/images/ajax-loader.gif',
			scrollZoom : true,
			borderSize: 1
		});

    var content = $("#prod_desc").text();
    $("#prod_desc").html(textile.convert(content));
		
	});

})();