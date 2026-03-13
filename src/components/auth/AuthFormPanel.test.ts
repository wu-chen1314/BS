import { mount } from "@vue/test-utils";
import AuthFormPanel from "./AuthFormPanel.vue";
import { elementStubs } from "@/test-utils/element-stubs";

const baseForm = {
  username: "",
  passwordHash: "",
  confirmPassword: "",
  nickname: "",
  email: "",
  code: "",
  captchaAnswer: "",
};

describe("AuthFormPanel", () => {
  it("renders login mode copy and emits forgot password", async () => {
    const wrapper = mount(AuthFormPanel, {
      props: {
        form: { ...baseForm },
        rules: {},
        isLogin: true,
        loading: false,
        regCountdown: 0,
        captchaQuestion: "1 + 2 = ?",
      },
      global: {
        stubs: elementStubs,
      },
    });

    expect(wrapper.text()).toContain("欢迎回来");
    expect(wrapper.text()).toContain("忘记密码");

    const usernameInput = wrapper.find('input[placeholder="用户名（字母 / 数字 / 下划线）"]');
    await usernameInput.trigger("focus");
    expect(wrapper.emitted("focus-field")?.[0]).toEqual(["username"]);

    await usernameInput.trigger("blur");
    expect(wrapper.emitted("blur-field")).toHaveLength(1);

    await wrapper.get(".login-btn").trigger("click");
    expect(wrapper.emitted("submit")).toHaveLength(1);

    const forgotLink = wrapper.findAll("a").find((item) => item.text().includes("忘记密码"));
    expect(forgotLink).toBeTruthy();
    await forgotLink!.trigger("click");
    expect(wrapper.emitted("open-forgot")).toHaveLength(1);
  });

  it("renders register mode and emits send code", async () => {
    const wrapper = mount(AuthFormPanel, {
      props: {
        form: { ...baseForm },
        rules: {},
        isLogin: false,
        loading: false,
        regCountdown: 0,
        captchaQuestion: "2 + 3 = ?",
      },
      global: {
        stubs: elementStubs,
      },
    });

    expect(wrapper.text()).toContain("创建账号");
    expect(wrapper.text()).toContain("直接登录");

    const emailInput = wrapper.find('input[placeholder="邮箱地址"]');
    await emailInput.trigger("focus");
    expect(wrapper.emitted("focus-field")?.at(-1)).toEqual(["email"]);

    await wrapper.get(".code-btn").trigger("click");
    expect(wrapper.emitted("send-register-code")).toHaveLength(1);
  });
});
