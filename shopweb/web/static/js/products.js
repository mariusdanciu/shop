(function() {
	$(function() {

		$.blockUI.defaults.baseZ = 90;

		refreshList();

		$("#sortSelect").chosen({
			"disable_search" : true
		});

		$('#sortSelect').on(
				'change',
				function(evt, params) {
					$("#item_list").load(
							normUrl("/products", $(this).val()),
							function(response, status, xhr) {
								if (status == "error") {
									$("#notice_connect_e").show().delay(5000)
											.fadeOut("slow");
								} else {
									refreshList();
								}
							});
				})

		$(".close_dialog").bind("click", function(event) {
			closeDialog();
		});

		$('#create_product_tab').tabify();

		$("#create_product").click(function(event) {

			$("#upload_form").each(function() {
				var formData = new FormData(this);

				$('#upload_form label').css("color", "#000000").removeAttr("title");
				$.ajax({
					url : "/product/create",
					type : "POST",
					cache: false,
				    contentType: false,
				    processData: false,
					data : formData
				}).fail(function(msg, f) {
					$("#notice_connect_e").show().delay(5000).fadeOut("slow");
				});
			});

			
			event.stopPropagation();
			event.preventDefault();
		});

		$("#itemadd").click(function(event) {
			$.blockUI({
				message : $("#product_create_dialog"),
				css : {
					top : '70px',
					left : ($(window).width() - 800) / 2 + 'px',
					width : '800px',
					border : 'none',
					cursor : null
				},
				overlayCSS : {
					cursor : null,
					backgroundColor : '#dddddd'
				}
			});
		});

		$("#create_description").jqte({
			source : false
		});

		$("#add_prop").click(function(event) {
			var div = $("<div class='row'></div>");
			div.append("<input type='text' name='pkey'/><input type='text' name='pval'/>");
			var remove = $("<img class='clickable' src='/static/images/minus.png'/>");
			remove.click(function(e){
				div.remove();
			});
			div.append(remove);
			
			$("#prop_fields").append(div);
		});
		
	});

	var normUrl = function(url, sort) {
		var cat = $.url().param("cat");
		var search = $.url().param("search");

		if (cat === undefined) {
			url += "?search=" + search;
		} else {
			url += "?cat=" + cat;
		}
		url += "&sort=" + sort;
		return url;
	};

	var closeDialog = function() {
		$.unblockUI();
		setTimeout(function() {
			$("#product_dialog").empty();
			$(".zoomContainer").remove();
		}, 400);
	}

	var refreshList = function() {
		$(".item_box")
				.each(
						function(index) {
							var me = $(this);

							me.find('.info_tag_cart').click(function(event) {
								var pid = me.attr("id");
								cart.addItem(pid);
								cart.showCart();
								event.stopPropagation();
							});

							var pid = me.attr("id");
							if (pid !== undefined) {
								me
										.click(function(event) {
											var loc = "/productquickview?pid="
													+ pid;
											$("#product_dialog")
													.load(
															loc,
															function(response,
																	status, xhr) {
																if (status == "error") {
																	$(
																			"#notice_connect_e")
																			.show()
																			.delay(
																					5000)
																			.fadeOut(
																					"slow");
																} else {

																	$(
																			"#sel_img")
																			.elevateZoom(
																					{
																						gallery : 'detail_box',
																						cursor : 'pointer',
																						galleryActiveClass : 'active',
																						imageCrossfade : true,
																						loadingIcon : '/images/ajax-loader.gif',
																						scrollZoom : true,
																						borderSize : 1
																					});

																	$(
																			'#add_to_cart')
																			.click(
																					function(
																							event) {
																						closeDialog();
																						cart
																								.addItem(pid);
																						cart
																								.showCart();
																						event
																								.stopPropagation();
																					});

																	$
																			.blockUI({
																				message : $("#product_dialog"),
																				css : {
																					top : '150px',
																					left : ($(
																							window)
																							.width() - 1000)
																							/ 2
																							+ 'px',
																					width : '1000px',
																					border : 'none',
																					cursor : null
																				},
																				overlayCSS : {
																					cursor : null,
																					backgroundColor : '#dddddd'
																				}
																			});

																	$(
																			".close_dialog")
																			.bind(
																					"click",
																					function(
																							event) {
																						closeDialog();
																					});
																}
															});
											event.stopPropagation();
										});
							}
						});
	}

})();