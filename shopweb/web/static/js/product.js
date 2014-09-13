(function() {
	$(function() {

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

	});

})();