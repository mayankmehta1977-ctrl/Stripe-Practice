from dataclasses import dataclass, field


@dataclass
class LineItem:
    description: str
    unit_amount: int        # in the currency's minor unit (see API.md)
    quantity: int = 1

    @property
    def amount(self) -> int:
        return self.unit_amount * self.quantity


@dataclass
class Invoice:
    id: str
    customer_id: str
    currency: str           # lowercase ISO code, e.g. "usd"
    status: str             # "draft" | "open" | "paid" | "void"
    line_items: list[LineItem] = field(default_factory=list)

    @property
    def total(self) -> int:
        return sum(item.amount for item in self.line_items)

    @property
    def is_paid(self) -> bool:
        return self.status == "paid"
