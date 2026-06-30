"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const bodyFront_1 = require("./assets/bodyFront");
const bodyBack_1 = require("./assets/bodyBack");
const bodyFemaleFront_1 = require("./assets/bodyFemaleFront");
const bodyFemaleBack_1 = require("./assets/bodyFemaleBack");
const SvgMaleWrapper_1 = require("./components/SvgMaleWrapper");
const SvgFemaleWrapper_1 = require("./components/SvgFemaleWrapper");
const Body = ({ colors = ["#0984e3", "#74b9ff"], data, scale = 1, side = "front", gender = "male", onBodyPartPress, border = "#dfdfdf", disabledParts = [], hiddenParts = [], defaultFill = "#3f3f3f", defaultStroke = "none", defaultStrokeWidth = 0, }) => {
    const getPartStyles = (0, react_1.useCallback)((bodyPart) => {
        // Per-part styles override global defaults
        return {
            fill: bodyPart.styles?.fill ?? defaultFill,
            stroke: bodyPart.styles?.stroke ?? defaultStroke,
            strokeWidth: bodyPart.styles?.strokeWidth ?? defaultStrokeWidth,
        };
    }, [defaultFill, defaultStroke, defaultStrokeWidth]);
    const mergedBodyParts = (0, react_1.useCallback)((dataSource) => {
        const filteredDataSource = dataSource.filter((part) => !hiddenParts.includes(part.slug));
        // Create a map of user data by slug for faster lookup
        const userDataMap = new Map();
        data.forEach((userPart) => {
            if (userPart.slug) {
                userDataMap.set(userPart.slug, userPart);
            }
        });
        // Merge asset body parts with user data
        return filteredDataSource.map((assetPart) => {
            const userPart = userDataMap.get(assetPart.slug);
            if (!userPart) {
                // No user data for this part, return as-is
                return assetPart;
            }
            // Merge asset part (has path) with user part (has styles, color, etc.)
            const merged = {
                ...assetPart,
                // Explicitly copy user properties
                styles: userPart.styles,
                intensity: userPart.intensity,
                side: userPart.side,
                color: userPart.color,
            };
            // Set color fallback based on intensity if provided
            if (!merged.styles?.fill && !merged.color && merged.intensity) {
                merged.color = colors[merged.intensity - 1];
            }
            return merged;
        });
    }, [data, colors, hiddenParts]);
    const getColorToFill = (bodyPart) => {
        if (bodyPart.slug && disabledParts.includes(bodyPart.slug)) {
            return "#EBEBE4";
        }
        // Priority: per-part styles.fill > color prop > intensity-based color > default
        if (bodyPart.styles?.fill) {
            return bodyPart.styles.fill;
        }
        if (bodyPart.color) {
            return bodyPart.color;
        }
        if (bodyPart.intensity && bodyPart.intensity > 0) {
            return colors[bodyPart.intensity - 1];
        }
        return undefined; // Let getPartStyles provide the default
    };
    const isPartDisabled = (slug) => slug && disabledParts.includes(slug);
    const renderBodySvg = (bodyToRender) => {
        const SvgWrapper = gender === "male" ? SvgMaleWrapper_1.SvgMaleWrapper : SvgFemaleWrapper_1.SvgFemaleWrapper;
        return ((0, jsx_runtime_1.jsx)(SvgWrapper, { side: side, scale: scale, border: border, children: mergedBodyParts(bodyToRender).map((bodyPart) => {
                const commonPaths = (bodyPart.path?.common || []).map((path, index) => {
                    const partStyles = getPartStyles(bodyPart);
                    const fillColor = getColorToFill(bodyPart);
                    return ((0, jsx_runtime_1.jsx)("path", { onClick: isPartDisabled(bodyPart.slug)
                            ? undefined
                            : () => onBodyPartPress?.(bodyPart), style: {
                            cursor: isPartDisabled(bodyPart.slug)
                                ? "not-allowed"
                                : "pointer",
                            opacity: isPartDisabled(bodyPart.slug) ? 0.6 : 1,
                        }, "aria-disabled": isPartDisabled(bodyPart.slug), id: bodyPart.slug, fill: fillColor ?? partStyles.fill, stroke: partStyles.stroke, strokeWidth: partStyles.strokeWidth, d: path }, `${bodyPart.slug}-common-${index}`));
                });
                const leftPaths = (bodyPart.path?.left || []).map((path, index) => {
                    const isOnlyRight = data.find((d) => d.slug === bodyPart.slug)?.side === "right";
                    const partStyles = getPartStyles(bodyPart);
                    const fillColor = isOnlyRight
                        ? defaultFill
                        : getColorToFill(bodyPart);
                    return ((0, jsx_runtime_1.jsx)("path", { onClick: isPartDisabled(bodyPart.slug)
                            ? undefined
                            : () => onBodyPartPress?.(bodyPart, "left"), style: {
                            cursor: isPartDisabled(bodyPart.slug)
                                ? "not-allowed"
                                : "pointer",
                            opacity: isPartDisabled(bodyPart.slug) ? 0.6 : 1,
                        }, id: bodyPart.slug, fill: fillColor ?? partStyles.fill, stroke: partStyles.stroke, strokeWidth: partStyles.strokeWidth, d: path }, `${bodyPart.slug}-left-${index}`));
                });
                const rightPaths = (bodyPart.path?.right || []).map((path, index) => {
                    const isOnlyLeft = data.find((d) => d.slug === bodyPart.slug)?.side === "left";
                    const partStyles = getPartStyles(bodyPart);
                    const fillColor = isOnlyLeft
                        ? defaultFill
                        : getColorToFill(bodyPart);
                    return ((0, jsx_runtime_1.jsx)("path", { onClick: isPartDisabled(bodyPart.slug)
                            ? undefined
                            : () => onBodyPartPress?.(bodyPart, "right"), style: {
                            cursor: isPartDisabled(bodyPart.slug)
                                ? "not-allowed"
                                : "pointer",
                            opacity: isPartDisabled(bodyPart.slug) ? 0.6 : 1,
                        }, id: bodyPart.slug, fill: fillColor ?? partStyles.fill, stroke: partStyles.stroke, strokeWidth: partStyles.strokeWidth, d: path }, `${bodyPart.slug}-right-${index}`));
                });
                return [...commonPaths, ...leftPaths, ...rightPaths];
            }) }));
    };
    const bodyToRender = gender === "female"
        ? side === "front"
            ? bodyFemaleFront_1.bodyFemaleFront
            : bodyFemaleBack_1.bodyFemaleBack
        : side === "front"
            ? bodyFront_1.bodyFront
            : bodyBack_1.bodyBack;
    return renderBodySvg(bodyToRender);
};
exports.default = Body;
//# sourceMappingURL=index.js.map