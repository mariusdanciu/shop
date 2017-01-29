(function() {
	$(function() {
		var galleryOffset = 0;
		var total = $("#gallery ul li").length;

		$("#add_to_cart_box").click(function(event) {
			var pid = $(this).attr("data-id");
			cart.addItem(pid);
            window.common.showNotice("Produsul a fost adăugat in coș.")
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

		$("#del_prod").click(function(event) {
			var id = window.location.pathname.split('/')[2];
			window.admin.deleteProduct(id, function() {
				window.location.href = "/";
			});
			event.stopPropagation();
			event.preventDefault();
			return false;
		});

	});

})();