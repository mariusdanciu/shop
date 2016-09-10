(function() {
	$(function() {

	});

})();

var admin = {

	deleteUser : function(email, okFunc) {
		$.ajax({
			url : "delete/user/" + email,
			type : "DELETE",
			cache : false,
			timeout : 3000,
			statusCode : {
				200 : function(msg) {
					okFunc();
				},

				403 : function(msg) {
					var data = JSON.parse(msg.responseText);
					if (data.errors) {
						common.showFormErrors(data.errors);
					}
				}
			}
		});
	},

	users : function(selector) {
		$(selector).load("/usersview", function(response, status, xhr) {
			if (status === "error") {
				common.showError(xhr.statusText);
			} else {
				settings.refreshAccordion();

				$(".delusr").click(function(e) {
					var email = $(this).attr("data-email");
					window.admin.deleteUser(email, function() {
						console.log(email);
						window.admin.users(selector);
					});
					return false;
				})
			}
		});
	},

	saveCategory : function(formId) {
		window.admin.save(formId, function() {
			window.categories.reloadCategories();
		});
	},

	deleteProduct : function(id) {
		$.ajax({
			cache : false,
			url : "/product/delete/" + id,
			timeout : 3000,
			type : "DELETE"
		}).success(products.reloadProducts);
	},

	getCategory : function(id, categoryFunc) {
		$.ajax({
			cache : false,
			url : "/category/" + id,
			timeout : 3000,
			type : "GET",
			dataType : "json"
		}).success(categoryFunc);
	},

	deleteCategory : function(id) {
		$.ajax({
			cache : false,
			url : "/category/delete/" + id,
			timeout : 3000,
			type : "DELETE",
		}).success(categories.reloadCategories());
	},

	save : function(formId, successFunc) {
		$(formId).each(function() {
			var frm = this;
			var formData = new FormData(frm);

			if (formData.has && formData.has("edit_categories")) {
				formData.set("edit_categories", $("#edit_categories").val());
			}

			$(formId + ' label').css("color", "#555555").removeAttr("title");
			$.ajax({
				url : $(frm).attr('action'),
				type : "POST",
				cache : false,
				contentType : false,
				processData : false,
				timeout : 10000,
				data : formData,
				statusCode : {
					201 : function(data) {
						successFunc(data);
					},
					403 : function(msg) {
						var data = JSON.parse(msg.responseText);
						if (data.errors) {
							common.showFormErrors(data.errors);
						}
						return false;
					}
				}
			});
		});
	},

	toggleDescription : function(prefix) {
		$("#" + prefix + "_toggle_desc").click(
				function(e) {
					$("." + prefix + "_toggle_text").toggle();
					$("." + prefix + "_toggle").toggle(
							{
								duration : 0,
								done : function() {
									var preview = $("#" + prefix
											+ "_description_view");
									if (preview.css('display') != 'none') {
										preview.html(textile.convert($(
												"#" + prefix + "_description")
												.val()));
									}
								}
							});
					return false;
				});

	}

};