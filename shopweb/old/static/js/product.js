(function() {
	$(function() {
		var galleryOffset = 0;
		var total = $("#gallery ul li").length;
		if (total > 3) {
			$('#gallery_right').click(function() {
				if (galleryOffset * -4 < total * 100) {
					galleryOffset -= 100;
				}
				$("#gallery ul").css({
					"transform" : "translate(" + galleryOffset + "px, 0px)"
				});
				return false;
			});

			$('#gallery_left').click(function() {
				if (galleryOffset * 4 < 0) {
					galleryOffset += 100;
				}
				$("#gallery ul").css({
					"transform" : "translate(" + galleryOffset + "px, 0px)"
				});
				return false;
			});
		}

		if (window.admin) {
			window.admin.attachToProduct($("body"), function() {
				window.location.reload();
			});
		}

		console.log($("#add_to_cart_box"));
		$("#add_to_cart_box").click(function(event) {
			console.log("Add");
			var pid = window.location.pathname.split('/')[2];
			console.log(pid);
			cart.addItem(pid);
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

		$("#fb-share-button").click(function(e) {
			var pid = window.location.pathname.split('/')[2];
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

		$('#product_details_tab').tabify();
	});

})();