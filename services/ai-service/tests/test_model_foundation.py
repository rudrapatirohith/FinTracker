
import uuid

from sqlalchemy import DateTime

from app.models.base import (
    Base,
    TimestampMixin,
    UUIDPrimaryKey,
)


def test_base_is_sqlalchemy_declarative_base():
    assert Base.metadata is not None


def test_uuid_primary_key_uses_uuid_type():
    class ExampleModel(UUIDPrimaryKey, Base):
        __tablename__ = "example_model_phase2_test"

    column = ExampleModel.__table__.c.id

    assert column.primary_key is True
    assert column.default is not None
    # Check that the default function is indeed uuid4 by checking its callable name
    assert column.default.arg.__name__ == "uuid4"
    

def test_timestamp_columns_are_timezone_aware():
    class ExampleTimestampModel(UUIDPrimaryKey,  TimestampMixin, Base):
        __tablename__ = "example_timestamp_phase2_test"

        from sqlalchemy.orm import Mapped, mapped_column
        from sqlalchemy import Integer

        value: Mapped[int] = mapped_column(Integer, nullable=False)

    created_at = ExampleTimestampModel.__table__.c.created_at
    updated_at = ExampleTimestampModel.__table__.c.updated_at

    assert isinstance(created_at.type, DateTime)
    assert isinstance(updated_at.type, DateTime)
    assert created_at.type.timezone is True
    assert updated_at.type.timezone is True
