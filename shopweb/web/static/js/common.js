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

        $("#menu").tabify();

        $("#cart_symbol").click(function(event) {
            window.cart.showCart();
            return false;
        });

        $("#user_symbol").click(function(event) {
            window.user.showLogin();
            return false;
        });

        $("#cart_popup, #user_popup, #newuser_popup").click(function(event) {
            event.stopPropagation();
        });

        $("#authgo").click(function(event) {
            window.user.login("#login_form");
            return false;
        });

        $("#newuser").click(function(event) {
            window.user.hideLogin();
            window.user.showNewUser();
            return false;
        });

        $("#save_user").click(function(event) {
            window.user.createUser("#newuser_form");
            return false;
        });

        $('#forgotpass').click(function(event) {
            window.user.forgotPass("#login_form");
            return false;
        });

        $("#login_form").keydown(function(event) {
            if (event.keyCode == 13) {
                window.user.login("#login_form");
                return false;
            }
        });

        $("#logout").click(function(event) {
            window.user.logout();
            event.stopPropagation();
            event.preventDefault();
            return false;
        });

        $(".close_dialog").bind("click", function(event) {
            common.closeDialog();
            return false;
        });

        $(document).click(function() {
            cart.hideCart();
            user.hideLogin();
        });

        $(document).keyup(function(e) {
            if (e.keyCode == 27) {
                common.closeDialog();
                cart.hideCart();
                user.hideLogin();
            }
        });

        $("#buy_step0").click(function(event) {
            cart.fetchUserInfo();
            cart.showStep0Links();
            return false;
        });

        $("#buy_step1").click(function(event) {
            cart.fetchUserInfo();
            cart.showStep1Links();
            return false;
        });

        $('#buy_final, #c_buy_final_comp').click(function(event) {
            var clicked = $(this).attr("id");
            var form = (clicked !== "c_buy_final_comp") ? "#order_form" : "#order_form_company";

            var formObj = {};
            var obj = $(form).serializeArray();

            $.each(obj, function() {
                formObj[this.name] = this.value;
            });

            var items = cart.items();
            formObj["items"] = [];
            for (e in items) {
                formObj["items"].push({
                    name : items[e].id,
                    userOptions : items[e].userOptions,
                    value : items[e].count
                })
            }

            cart.cleanFormMessages();
            $.ajax({
                url : "/order",
                contentType : 'application/json; charset=UTF-8',
                data : JSON.stringify(formObj),
                cache : false,
                timeout : 3000,
                type : 'POST',
                statusCode : {
                    403 : function(msg) {
                        var data = JSON.parse(msg.responseText);
                        if (data.errors) {
                            common.showFormErrors(data.errors);
                        }
                    }
                }
            });
            return false;
        });

        $("#search_text").keypress(function(e) {
            if (e.which == 13) {
                var text = $("#search_text").val();
                window.location.href = '/products?search=' + text;
            }
        });

        $("#transport_pf").change(function() {
            cart.updateSubmitText("pf");
        });

        $("#transport_pj").change(function() {
            cart.updateSubmitText("pj");
        });

        window.cart.loadView();

        FB.init({
            appId : '915281988516999',
            status : true,
            xfbml : true,
            version : 'v2.0'
        });
    });

})();

var common = {

    closeDialog : function() {
        $.unblockUI();
        $("#user_popup").hide();
    },

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
            $("label[for='" + this.id + "']").css("color", "#ff0000").attr("title", this.error);
        });
    }

}

var user = {
    showNewUser : function() {
        $.blockUI({
            message : $("#newuser_popup"),
            css : {
                top : '150px',
                left : ($(window).width() - 630) / 2 + 'px',
                width : '630px',
                border : 'none',
                cursor : null
            },
            overlayCSS : {
                cursor : null,
                backgroundColor : '#dddddd'
            }
        });
    },

    showLogin : function() {
        $("#user_popup").show();
    },

    hideLogin : function() {
        $("#user_popup").hide();
    },

    logout : function() {
        window.location.href = "/?logout=true";
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
            common.closeDialog();
            common.showNotice(m);
        });
    },

    login : function(frmId) {
        var creds = $.base64.encode($(frmId + " #username").val() + ":" + $(frmId + " #password").val());

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
                    common.closeDialog();
                    common.showError(m.responseText);
                }
            }
        });
    },

    createUser : function(formId) {
        $(formId).each(function() {
            var frm = this;

            $(formId + ' label').css("color", "#555555").removeAttr("title");
            $.ajax({
                url : $(formId).attr('action'),
                type : "POST",
                cache : false,
                timeout : 3000,
                data : $(formId).serialize(),
                statusCode : {
                    201 : function() {
                        window.location.href = "/";
                    },
                    403 : function(msg) {
                        var data = JSON.parse(msg.responseText);
                        if (data.errors) {
                            common.showFormErrors(data.errors);
                        }
                    }
                }
            });
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
        $("#order_form #region, #order_form_company #cregion").attr("value", addresses[idx].region);
        $("#order_form #city, #order_form_company #ccity").attr("value", addresses[idx].city);
        $("#order_form #address, #order_form_company #caddress").attr("value", addresses[idx].address);
        $("#order_form #zip, #order_form_company #czip").attr("value", addresses[idx].zipCode);
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
        $("#order_form_company #cregcom").attr("value", data.companyInfo.regCom);
        $("#order_form_company #cbank").attr("value", data.companyInfo.bank);
        $("#order_form_company #cbankaccount").attr("value", data.companyInfo.bankAccount);
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
        $('#order_form label, #order_form_company label').css("color", "#555555").removeAttr("title");
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

    addItem : function(id, userOpts) {
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

            if (!userOpts) {
                userOpts = {};
            }

            if (!found || JSON.stringify(found.userOptions) !== JSON.stringify(userOpts)) {
                cart.items.push({
                    id : id,
                    userOptions : userOpts,
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
                    userOptions : {},
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
        var tr = $("#transport_" + suffix).val();
        var transport = 19.99;
        if (tr == "2") {
            transport = 9.99;
        }
        var pret = cart.computeTotal(transport);
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
