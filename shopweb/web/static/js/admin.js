(function() {
    $(function() {
        $('#create_product_tab').tabify();

        $("#update_category_form").keydown(function(event) {
            if (event.keyCode == 13) {
                window.admin.saveCategory("#update_category_form");
                return false;
            }
        });

        $("#create_category_form").keydown(function(event) {
            if (event.keyCode == 13) {
                window.admin.saveCategory("#create_category_form");
                return false;
            }
        });

        $("#create_category").click(function(event) {
            window.admin.saveCategory("#create_category_form");
            return false;
        });

        $("#update_category").click(function(event) {
            window.admin.saveCategory("#update_category_form");
            return false;
        });

        $("#create_product").click(function(event) {
            window.admin.save("#upload_form", function() {
                window.common.closeDialog();
                window.products.reloadProducts();
            });
            return false;
        });

        $("#search_orders").keydown(function(event) {
            if (event.keyCode == 13) {
                admin.searchOrders("#orders_result", $("#search_orders").val());
                return false;
            }
        });

        $("#search_received_orders").click(function(event) {
            admin.receivedOrders("#pending_orders_result");
            return false;
        });
        
        $("#search_pending_orders").click(function(event) {
            admin.pendingOrders("#pending_orders_result");
            return false;
        });
        

        admin.addProp("#add_prop", "#prop_fields", "pkey", "pval");

        admin.addText("#add_custom_text", "#prop_custom_fields", "customtext");
        admin.addProp("#add_custom_prop", "#prop_custom_fields", "customkey", "customval");

        admin.toggleDescription("create");

        document.onkeydown = function(evt) {
            evt = evt || window.event;

            if (evt.keyCode == 27) {
                if (window.products !== undefined) {
                    window.common.closeDialog();
                }
                if (window.categories !== undefined) {
                    window.common.closeDialog();
                }
            }
        };
    });

})();

var admin = {

    receivedOrders : function(selector) {
        $(selector).load("/ordersview/received", function(response, status, xhr) {
            if (status === "error") {
                common.showError(xhr.statusText);
            } else {
                settings.refreshAccordion();
            }
        });
    },
    
    pendingOrders : function(selector) {
        $(selector).load("/ordersview/pending", function(response, status, xhr) {
            if (status === "error") {
                common.showError(xhr.statusText);
            } else {
                settings.refreshAccordion();
            }
        });
    },

    searchOrders : function(selector, email) {
        $(selector).load("/ordersview?email=" + email, function(response, status, xhr) {
            if (status === "error") {
                common.showError(xhr.statusText);
            } else {
                settings.refreshAccordion();
            }
        });
    },

    saveCategory : function(formId) {
        window.admin.save(formId, function() {
            window.common.closeDialog();
            window.categories.reloadCategories();
        });
    },

    addProp : function(elem, holder, key, val) {
        $(elem).click(function(event) {
            var div = $("<div class='row'></div>");
            div.append("<input type='text' name='" + key + "'/><input type='text' name='" + val + "'/>");

            var remove = $("<span class='clickable sprite sprite-minus top5'/>");
            remove.click(function(e) {
                div.remove();
                return false;
            });

            div.append(remove);
            $(holder).append(div);
            return false;

        });
    },

    addText : function(elem, holder, key) {
        $(elem).click(function(event) {
            var div = $("<div class='row'></div>");
            div.append("<input type='text' name='" + key + "'/>");
            var remove = $("<span class='clickable sprite sprite-minus top5'/>");
            remove.click(function(e) {
                div.remove();
                return false;
            });

            div.append(remove);
            $(holder).append(div);
            return false;

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
                    201 : function() {
                        successFunc();
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
        $("#" + prefix + "_toggle_desc").click(function(e) {
            $("." + prefix + "_toggle_text").toggle();
            $("." + prefix + "_toggle").toggle({
                duration : 0,
                done : function() {
                    var preview = $("#" + prefix + "_description_view");
                    if (preview.css('display') != 'none') {
                        preview.html(textile.convert($("#" + prefix + "_description").val()));
                    }
                }
            });
            return false;
        });

    },

    attachToProduct : function(successFunc) {
        $("#save_product").click(function(event) {
            admin.save("#edit_form", successFunc);
            return false;
        });
        $('#edit_product_tab').tabify();
        $("#edit_specs .row span, #prop_edit_custom_fields .row span").click(function(e) {
            var row = $(this).parent();
            row.remove();
            return false;
        });

        admin.addProp("#edit_add_prop", "#edit_prop_fields", "pkey", "pval");
        admin.addText("#add_edit_custom_text", "#prop_edit_custom_fields", "customtext");
        admin.addProp("#add_edit_custom_prop", "#prop_edit_custom_fields", "customkey", "customval");

        admin.toggleDescription("edit");
    },

    attachCreateProduct : function(elem) {
        elem.click(function(event) {
            $.blockUI({
                message : $("#product_create_dialog"),
                css : {
                    top : '70px',
                    left : ($(window).width() - 580) / 2 + 'px',
                    width : '580px',
                    border : 'none',
                    cursor : null
                },
                overlayCSS : {
                    cursor : null,
                    backgroundColor : '#dddddd'
                }
            });
            return false;
        });
    },

    attachCreateCategory : function(elem) {
        elem.click(function(event) {
            $.blockUI({
                message : $("#category_create_dialog"),
                css : {
                    top : '200px',
                    left : ($(window).width() - 320) / 2 + 'px',
                    width : '320px',
                    border : 'none',
                    cursor : null
                },
                overlayCSS : {
                    cursor : null,
                    backgroundColor : '#dddddd'
                }
            });
            return false;
        });
    },

    editCategory : function(cid) {
        admin.getCategory(cid, function(obj) {
            $('#update_category_form #title').val(obj.title);
            $('#update_category_form #pos').val(obj.position);

            $('#update_category_form').attr("action", "/category/update/" + cid)
            $.blockUI({
                message : $("#category_update_dialog"),
                css : {
                    top : '200px',
                    left : ($(window).width() - 320) / 2 + 'px',
                    width : '320px',
                    border : 'none',
                    cursor : null
                },
                overlayCSS : {
                    cursor : null,
                    backgroundColor : '#dddddd'
                }
            });
        });

    }

};