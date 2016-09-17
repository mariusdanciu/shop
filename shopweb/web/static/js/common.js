(function() {
	$(function() {

		window.submitText = $(".submit_pf").text();

		if (!window.console) {
			var console = {
				log : function() {
				},
				warn : function() {
				},
				error : function() {
				},
				time : function() {
				},
				timeEnd : function() {
				}
			}
		}

		var search = $.url().param("search");
		if (search !== undefined) {
			$(".search_text").val(search);
		}

		$(document).ajaxError(function(event, request, settings, thrownError) {

			if (thrownError == "") {
				common.showConnectionError();
			} else {
				var ct = request.getResponseHeader("content-type") || "";
				if (ct.indexOf('text') > -1) {
					common.showError(request.responseText);
				}
			}
		});

		$("#login_form").keydown(function(event) {
			if (event.keyCode == 13) {
				window.user.login("#login_form");
				return false;
			}
		});

		FB.init({
			appId : '915281988516999',
			status : true,
			xfbml : true,
			version : 'v2.0'
		});
	});

})();

var common = {

	showNotice : function(text) {
		$("#notice_i").html(text);
		$("#notice_i").show().delay(5000).fadeOut("slow");
	},

	showError : function(text) {
		$("#notice_e").html(text);
		$("#notice_e").show().delay(5000).fadeOut("slow");
	},

	showConnectionError : function() {
		$("#notice_connect_e").show().delay(5000).fadeOut("slow");
	},

	showFormErrors : function(errors) {
		$.each(errors, function() {
			$("label[for='" + this.id + "']").css("color", "#ff0000").attr(
					"title", this.error);
		});
	},

}

var user = {
		
	login : function(frmId) {
		var creds = $.base64.encode($(frmId + " #username").val() + ":"
				+ $(frmId + " #password").val());

		$.ajax({
			url : $(frmId).attr('action'),
			type : "GET",
			cache : false,
			timeout : 3000,
			headers : {
				'Authorization' : "Basic " + creds
			},
			statusCode : {
				200 : function() {
					window.location.href = "/";
				},
				
				406 : function(m) {
					common.showError(m.responseText);
				}
			}
		});
	},

}

var addresses = undefined;
var currentAddress = 0;

var cart = {

	fetchUserInfo : function() {
		$.ajax({
			url : "/userinfo",
			dataType : "json",
			timeout : 3000,
			cache : false,
			context : $("#cart_content")
		}).done(function(data) {
			cart.populateForm(data);
		});
	},

	populateAddress : function(idx) {
		$("#order_form #region, #order_form_company #cregion").attr("value",
				addresses[idx].region);
		$("#order_form #city, #order_form_company #ccity").attr("value",
				addresses[idx].city);
		$("#order_form #address, #order_form_company #caddress").attr("value",
				addresses[idx].address);
		$("#order_form #zip, #order_form_company #czip").attr("value",
				addresses[idx].zipCode);
		$(".address_name").html(addresses[idx].name);
	},

	populateForm : function(data) {
		$("#order_form #fname").attr("value", data.userInfo.firstName);
		$("#order_form #lname").attr("value", data.userInfo.lastName);
		$("#order_form #email").attr("value", data.email);
		$("#order_form #phone").attr("value", data.userInfo.phone);
		$("#order_form #cnp").attr("value", data.userInfo.cnp);

		$("#order_form_company #cname").attr("value", data.companyInfo.name);
		$("#order_form_company #cif").attr("value", data.companyInfo.cif);
		$("#order_form_company #cregcom")
				.attr("value", data.companyInfo.regCom);
		$("#order_form_company #cbank").attr("value", data.companyInfo.bank);
		$("#order_form_company #cbankaccount").attr("value",
				data.companyInfo.bankAccount);
		$("#order_form_company #cemail").attr("value", data.email);
		$("#order_form_company #cphone").attr("value", data.companyInfo.phone);

		addresses = data.addresses;
		if (addresses && addresses.length > 0) {
			$(".address_nav").show();
			cart.populateAddress(currentAddress);

			$(".right_arrow").click(function(e) {
				if (currentAddress < addresses.length - 1) {
					currentAddress++;
					cart.populateAddress(currentAddress);
				}
				return false;
			});

			$(".left_arrow").click(function(e) {
				if (currentAddress > 0) {
					currentAddress--;
					cart.populateAddress(currentAddress);
				}
				return false;
			});
		} else {
			$("address_nav").hide();
		}

	},

	cleanFormMessages : function() {
		$('#order_form label, #order_form_company label').css("color",
				"#555555").removeAttr("title");
	},

	clear : function() {
		$.cookie("cart", JSON.stringify({
			items : []
		}));
		window.cart.loadView();
	},

	computeTotal : function(add) {
		var total = 0;

		$(".cart_item").each(function() {
			var num = $(this).find("input").val();
			var price = $(this).find(".cart_price").text();
			total += num * price;
		});
		if (add) {
			total += add;
		}

		return parseFloat(total).toFixed(2);
	},

	showTotal : function() {
		$("#total").text(cart.computeTotal() + " Lei");
	},

	items : function() {
		var c = $.cookie("cart");
		if (c) {
			return $.parseJSON(c).items;
		}
		return [];
	},

	addItem : function(id) {
		var c = $.cookie("cart");
		if (c) {
			var cart = $.parseJSON(c);
			var a = cart.items;
			var found = undefined;
			for (i in a) {
				if (a[i].id === id) {
					found = a[i];
				}
			}

			if (!found) {
				cart.items.push({
					id : id,
					count : 1
				});
			} else {
				found.count = found.count + 1;
			}
			$.cookie("cart", JSON.stringify(cart));
		} else {
			$.cookie("cart", JSON.stringify({
				items : [ {
					id : id,
					count : 1
				} ]
			}));
		}

	},

	setItemCount : function(pos, count) {
		var c = $.cookie("cart");
		if (c) {
			if (count === "")
				count = 1;

			var cart = $.parseJSON(c);
			var a = cart.items;
			for (i in a) {
				if (i == pos) {
					a[i].count = parseInt(count);
				}
			}
			$.cookie("cart", JSON.stringify(cart));
			window.cart.showTotal();
			$(this).focus();
		}
	},

	removeItem : function(pos) {
		var c = $.cookie("cart");
		if (c) {
			var cart = $.parseJSON(c);
			var a = cart.items;
			var na = [];
			for (i in a) {
				if (i != pos) {
					na.push(a[i])
				}
			}
			cart.items = na;
			$.cookie("cart", JSON.stringify(cart));
			window.cart.loadView();
		}
	},

	loadView : function(f) {

		if (window.cart.items().length === 0) {
			$('#order').hide();
			$('#cart_content').hide();
			$('#cart_footer').hide();
			$('#cart_empty').show();
			if (f !== undefined) {
				f();
			}
		} else {
			$('#cart_empty').hide();

			$.ajax({
				url : "/getcart",
				dataType : "json",
				timeout : 3000,
				cache : false,
				context : $("#cart_content")
			}).done(function(data) {
				$(this).empty();
				var ul = document.createElement("ul");

				for (var i = 0; i < data.length; i++) {
					var li = document.createElement("li");
					li.innerHTML = data[i];
					ul.appendChild(li);
				}

				$(this).append(ul);
				window.cart.showTotal();

				$('#cart_content').show();
				$('#cart_footer').show();

				$(".cart_item ul li input").each(function(index) {

					$(this).on("keyup change", function(e) {
						window.cart.setItemCount(index, $(this).val());
						e.preventDefault();
						return false;
					});
				});

				$(".del_cart_item a").each(function(index) {
					var me = $(this);
					var pos = me.attr("id").substring(4);
					me.click(function(e) {
						window.cart.removeItem(pos);
						e.preventDefault();
						return false;
					});
				});

				if (f !== undefined) {
					f();
				}
			});
		}
	},

	showStep0Links : function() {
		$('#cart_content').show();
		$('#order').hide();
		$('#buy_step1').show();
		$('#buy_step0').hide();
	},

	updateSubmitText : function(suffix) {
		var transp = $("#transport_pf").val();
		if (suffix === "pj")
			transp = $("#transport_pj").val();

		var pret = cart.computeTotal(parseFloat(transp));
		var text = window.submitText + " <b> " + pret + " Lei </b>";
		$(".submit_" + suffix).html(text);
	},

	showStep1Links : function() {

		cart.updateSubmitText("pf");
		cart.updateSubmitText("pj");

		$('#cart_content').hide();
		$('#order').show();
		$('#buy_step0').show();
		$('#buy_step1').hide();
	},

	showCart : function() {
		window.user.hideLogin();
		window.cart.loadView(function() {
			$('#order').hide();
			$('#cart_notice').hide();
			$('#buy_step0').hide();
			$('#buy_step1').show();
			$('#cart_popup').show();
		});

	},

	hideCart : function() {
		$('#cart_popup').hide();
		$('#cart_notice').hide();
		$('#order').hide();
		$('#buy_step1').show();
	}

}
