(function() {
	$(function() {
	})
})();

var addresses = undefined;
var currentAddress = 0;

var cart = {
	showFormErrors : function(errors) {
		$.each(errors, function() {
			$("label[for='" + this.id + "']").css("color", "#ff0000").attr(
					"title", this.error);
		});
	},

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
		}
	},
}

var common = {
	showNotice : function(text) {
		$("#notice_i").html(text);
		$("#notice_i").show().delay(5000).fadeOut("slow");
	},

	showError : function(text) {
		$("#notice_e").html(text);
		$("#notice_e").show().delay(5000).fadeOut("slow");
	},

	forgotPass : function(frmId) {
		var email = $.base64.encode($(frmId + " #username").val());
		$.ajax({
			url : "/forgotpassword/" + email,
			type : "POST",
			timeout : 3000,
			cache : false,
			statusCode : {
				404 : function(m) {
					common.closeDialog();
					common.showError(m.responseText);
				}
			}
		}).success(function(m) {
			common.showNotice(m);
		});
	},

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
					window.location.href = "/mobile";
				},
				406 : function(m) {
					window.common.showError(m.responseText);
				}
			}
		});
	},
	
    logout : function() {
        window.location.href = "/mobile?logout=true";
    },

}