import { defineComponent, h } from "vue";

const createSimpleStub = (tag = "div") =>
  defineComponent({
    inheritAttrs: false,
    props: {
      modelValue: {
        type: [String, Number, Boolean, Array, Object],
        default: "",
      },
      label: {
        type: String,
        default: "",
      },
      value: {
        type: [String, Number, Boolean, Object],
        default: "",
      },
      placeholder: {
        type: String,
        default: "",
      },
      type: {
        type: String,
        default: "",
      },
    },
    emits: ["click", "change", "update:modelValue"],
    setup(props, { attrs, emit, slots }) {
      if (tag === "input") {
        const { size, clearable, rows, prefixIcon, showPassword, ...restAttrs } = attrs as Record<string, unknown>;
        return () =>
          h(props.type === "textarea" ? "textarea" : "input", {
            ...restAttrs,
            value: props.modelValue as any,
            placeholder: props.placeholder,
            onInput: (event: Event) => emit("update:modelValue", (event.target as HTMLInputElement).value),
            onChange: (event: Event) => emit("change", (event.target as HTMLInputElement).value),
          });
      }

      if (tag === "button") {
        return () =>
          h(
            "button",
            {
              ...attrs,
              type: "button",
              onClick: (event: MouseEvent) => emit("click", event),
            },
            slots.default?.()
          );
      }

      if (tag === "a") {
        return () =>
          h(
            "a",
            {
              ...attrs,
              href: "#",
              onClick: (event: MouseEvent) => {
                event.preventDefault();
                emit("click", event);
              },
            },
            slots.default?.()
          );
      }

      if (tag === "checkbox") {
        return () =>
          h("label", attrs, [
            h("input", {
              type: "checkbox",
              checked: Boolean(props.modelValue),
              onChange: (event: Event) => {
                const checked = (event.target as HTMLInputElement).checked;
                emit("update:modelValue", checked);
                emit("change", checked);
              },
            }),
            ...(slots.default?.() || []),
          ]);
      }

      if (tag === "select") {
        return () =>
          h(
            "select",
            {
              ...attrs,
              value: props.modelValue as any,
              onChange: (event: Event) => emit("update:modelValue", (event.target as HTMLSelectElement).value),
            },
            slots.default?.()
          );
      }

      return () => h(tag, attrs, slots.default?.());
    },
  });

export const elementStubs = {
  "el-form": createSimpleStub("form"),
  "el-form-item": createSimpleStub("div"),
  "el-input": createSimpleStub("input"),
  "el-button": createSimpleStub("button"),
  "el-checkbox": createSimpleStub("checkbox"),
  "el-link": createSimpleStub("a"),
  "el-select": createSimpleStub("select"),
  "el-option": defineComponent({
    props: {
      label: { type: String, default: "" },
      value: { type: [String, Number], default: "" },
    },
    setup(props) {
      return () => h("option", { value: props.value }, props.label);
    },
  }),
  "el-tag": createSimpleStub("span"),
  "el-icon": createSimpleStub("span"),
  "el-empty": defineComponent({
    props: {
      description: { type: String, default: "" },
    },
    setup(props) {
      return () => h("div", { class: "el-empty-stub" }, props.description);
    },
  }),
  "el-avatar": createSimpleStub("div"),
  "el-dialog": createSimpleStub("div"),
  "el-upload": createSimpleStub("div"),
  "el-input-number": createSimpleStub("input"),
  "el-row": createSimpleStub("div"),
  "el-col": createSimpleStub("div"),
  "el-pagination": createSimpleStub("div"),
};
