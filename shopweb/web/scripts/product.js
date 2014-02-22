(function() {
	$(function() {

		$("#add_to_cart").click(function(event) {
			var pid = $.url().param("pid");
			cart.addItem(pid);
			cart.showCart();
			event.stopPropagation();
		});

		$(".small_img").click(function(event) {
			zoomFunc($(event.target));
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

		$("#sel_img").bind("click", function(e) {
			var ez = $('#sel_img').data('elevateZoom');
			$.fancybox(ez.getGalleryList());
			return false;
		});

	});

})();